"""
WOPI REST API 视图 - 实现与 Collabora Online 的集成
参考文档: https://wopi.readthedocs.io/
"""

import os
import hashlib
from django.http import HttpResponse, JsonResponse, FileResponse, Http404
from django.core.exceptions import ObjectDoesNotExist
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods
from django.utils.decorators import method_decorator
from django.views import View
from django.conf import settings

from django.http import JsonResponse
from .models import Document



# Collabora Online 服务配置
COLLABORA_URL = os.environ.get('COLLABORA_URL', 'http://localhost:9980')

# WOPI 外部访问地址（从 Collabora 容器访问 Django 的地址）
# 在 Docker 环境中，Collabora 容器通过服务名访问 Django
WOPI_EXTERNAL_URL = os.environ.get('WOPI_EXTERNAL_URL', None)


def get_document_or_404(doc_id):
    """获取文档或返回 404"""
    try:
        return Document.objects.get(id=doc_id)
    except (ObjectDoesNotExist, ValueError):
        from django.http import Http404
        raise Http404("Document not found")


def generate_access_token(doc_id, user_id='admin'):
    """生成访问令牌（简化版本，生产环境需更安全）"""
    token_str = f"{doc_id}:{user_id}:secret"
    return hashlib.sha256(token_str.encode()).hexdigest()


def verify_access_token(token, doc_id, user_id='admin'):
    """验证访问令牌"""
    expected = generate_access_token(doc_id, user_id)
    return token == expected


@method_decorator(csrf_exempt, name='dispatch')
class CheckFileInfo(View):
    """
    WOPI CheckFileInfo 接口
    获取文件信息，Collabora 会调用此接口获取文档元数据
    
    注意：根据 WOPI 规范，Lock/Unlock 等操作也是通过此 URL，
    使用 X-WOPI-Override 请求头来区分不同的操作类型。
    """
    
    def get(self, request, doc_id):
        # doc_id 现在是字符串，尝试转换为 UUID
        import uuid
        try:
            doc_uuid = uuid.UUID(doc_id)
        except ValueError:
            return JsonResponse({'error': 'Invalid document ID format'}, status=400)
        
        document = get_document_or_404(doc_uuid)
        
        # 构建 WOPI 外部访问 URL
        # 总是使用 host.docker.internal，因为这是 Collabora 容器访问 Django 的方式
        wopi_host = 'http://host.docker.internal:8000'
        
        # 构建文件信息响应 - 确保编辑权限
        file_info = {
            'BaseFileName': document.filename,
            'Size': document.file_size,
            'Version': str(document.version),
            'OwnerId': document.owner,
            'UserId': request.GET.get('user', 'admin'),
            'UserCanWrite': True,  # 始终允许写入
            'UserCanRename': False,
            'SupportsRename': False,
            'SupportsUpdate': True,
            'SupportsLocks': True,
            'SupportsContacts': False,
            'SupportsReviewing': True,
            'SupportsFiltering': False,
            'SupportedInsertButtonImageShapes': [],
            'HidePrintButton': False,
            'HideSaveButton': False,
            'HideExportButton': False,
            'DisablePreview': False,
            'FileUrl': f'{wopi_host}/wopi/files/{doc_id}',
            'FileSharingUrl': None,
            'UserFriendlyName': request.GET.get('user', 'Admin'),
            # 移除 WatermarkText，因为空值会导致 Collabora 解析错误
            'CloseUrl': f'{wopi_host}/wopi/close',
            'DownloadUrl': f'{wopi_host}/wopi/files/{doc_id}/contents',
        }
        
        # 设置 WOPI 响应头
        response = JsonResponse(file_info)
        response['X-WOPI-ItemVersion'] = str(document.version)
        response['X-WOPI-Lock'] = document.lock_token or ''
        
        return response
    
    def post(self, request, doc_id):
        """
        处理 WOPI 操作（Lock、Unlock、PutFile 等）
        通过 X-WOPI-Override 请求头来区分操作类型
        """
        override = request.headers.get('X-WOPI-Override', '')
        
        if override == 'LOCK':
            return self._lock(request, doc_id)
        elif override == 'UNLOCK':
            return self._unlock(request, doc_id)
        elif override == 'PUT':
            return self._put_file(request, doc_id)
        elif override == 'PUT_RELATIVE':
            return self._put_relative(request, doc_id)
        else:
            # 未知操作，返回 400
            return HttpResponse(status=400, content=f'Unknown X-WOPI-Override: {override}')
    
    def _lock(self, request, doc_id):
        """处理 LOCK 操作"""
        document = get_document_or_404(doc_id)
        
        lock_token = request.headers.get('X-WOPI-Lock', '')
        
        # 检查 OldLock 头（用于解锁后重新锁定）
        old_lock = request.headers.get('X-WOPI-OldLock', '')
        
        if old_lock:
            # 解锁后重新锁定
            if document.is_locked and document.lock_token != old_lock:
                response = HttpResponse(status=409)
                response['X-WOPI-Lock'] = document.lock_token or ''
                response['X-WOPI-LockFailureReason'] = 'Lock mismatch'
                return response
        else:
            # 普通锁定
            if document.is_locked:
                # 文件已被锁定，检查是否是同一个会话
                if document.lock_token and document.lock_token != lock_token:
                    response = HttpResponse(status=409)
                    response['X-WOPI-Lock'] = document.lock_token or ''
                    response['X-WOPI-LockFailureReason'] = 'File is already locked'
                    return response
                # 如果是同一个锁，允许重新锁定（Collabora 可能会重新发送相同的锁定请求）
        
        # 锁定文件（或更新锁）
        document.is_locked = True
        document.lock_token = lock_token
        document.save(update_fields=['is_locked', 'lock_token', 'updated_at'])
        
        response = HttpResponse(status=200)
        response['X-WOPI-Lock'] = lock_token
        response['X-WOPI-ItemVersion'] = str(document.version)
        
        return response
    
    def _unlock(self, request, doc_id):
        """处理 UNLOCK 操作"""
        document = get_document_or_404(doc_id)
        
        lock_token = request.headers.get('X-WOPI-Lock', '')
        
        if document.is_locked and document.lock_token != lock_token:
            response = HttpResponse(status=409)
            response['X-WOPI-Lock'] = document.lock_token or ''
            return response
        
        # 解锁文件
        document.is_locked = False
        document.lock_token = None
        document.save(update_fields=['is_locked', 'lock_token', 'updated_at'])
        
        response = HttpResponse(status=200)
        response['X-WOPI-ItemVersion'] = str(document.version)
        
        return response
    
    def _put_file(self, request, doc_id):
        """处理 PUT 操作（保存文件）"""
        document = get_document_or_404(doc_id)
        
        # 检查锁定状态
        lock_token = request.headers.get('X-WOPI-Lock', '')
        if document.is_locked and document.lock_token != lock_token:
            response = HttpResponse(status=409)
            response['X-WOPI-Lock'] = document.lock_token or ''
            response['X-WOPI-LockFailureReason'] = 'File is locked by another user'
            return response
        
        # 获取文件内容
        file_content = request.body
        if not file_content:
            return HttpResponse(status=400, content='No content to save')
        
        # 保存文件
        file_path = document.get_absolute_path()
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        
        with open(file_path, 'wb') as f:
            f.write(file_content)
        
        # 更新文档信息
        document.file_size = len(file_content)
        document.increment_version()
        
        response = HttpResponse(status=200)
        response['X-WOPI-ItemVersion'] = str(document.version)
        
        return response
    
    def _put_relative(self, request, doc_id):
        """处理 PUT_RELATIVE 操作（另存为新文件）"""
        # 简化实现：返回成功但不实际创建新文件
        document = get_document_or_404(doc_id)
        
        suggested_target = request.headers.get('X-WOPI-SuggestedTarget', '')
        
        response = HttpResponse(status=200)
        response['X-WOPI-ItemVersion'] = str(document.version)
        response['X-WOPI-Name'] = suggested_target or 'New Document'
        
        return response


class GetFile(View):
    """
    WOPI GetFile 接口
    获取文件内容，供 Collabora 打开文档
    """
    
    def get(self, request, doc_id):
        import logging
        logger = logging.getLogger(__name__)
        logger.error(f'GetFile called for doc_id={doc_id}')
        
        # doc_id 现在是字符串，尝试转换为 UUID
        import uuid
        try:
            doc_uuid = uuid.UUID(doc_id)
        except ValueError:
            logger.error(f'Invalid UUID format: {doc_id}')
            return HttpResponse(status=400, content='Invalid document ID format')
        
        # 添加详细日志
        full_url = request.build_absolute_uri()
        logger.error(f'Full request URL: {full_url}')
        logger.error(f'Remote address: {request.META.get("REMOTE_ADDR")}')
        
        # 验证访问令牌（如果提供了 token，则验证；否则允许访问）
        token = request.GET.get('access_token', '')
        logger.error(f'Token: {token}')
        
        # 验证 token 并显示期望值
        expected_token = generate_access_token(str(doc_uuid))
        logger.error(f'Expected token: {expected_token}')
        logger.error(f'Token match: {token == expected_token}')
        
        if token and not verify_access_token(token, str(doc_uuid)):
            logger.error('Invalid access token')
            return HttpResponse(status=401, content='Invalid access token')
        
        document = get_document_or_404(doc_uuid)
        
        # 检查文件是否存在
        file_path = document.get_absolute_path()
        logger.error(f'File path: {file_path}')
        
        if not os.path.exists(file_path):
            logger.error(f'File not found: {file_path}')
            return HttpResponse(status=404, content='File not found')
        
        # 返回文件内容
        response = FileResponse(
            open(file_path, 'rb'),
            content_type=document.mime_type
        )
        # 使用 inline 让 Collabora 可以在浏览器中直接显示
        response['Content-Disposition'] = f'inline; filename="{document.filename}"'
        response['X-WOPI-ItemVersion'] = str(document.version)
        
        logger.error(f'Successfully serving file: {file_path}')
        return response


@csrf_exempt
@require_http_methods(["GET", "POST"])
def wopi_discovery(request):
    """
    WOPI Discovery 接口
    返回 WOPI 客户端配置信息
    """
    
    # WOPI discovery XML (简化版本) - 支持 docx, xlsx, csv
    discovery_xml = f'''<?xml version="1.0" encoding="utf-8"?>
<wopi-discovery>
    <net-zone name="external-http">
        <app name="Word" favIconUrl="{COLLABORA_URL}/favicon.ico">
            <action name="view" ext="docx" urlsrc="{COLLABORA_URL}/hosting/discovery#word_view" />
            <action name="edit" ext="docx" urlsrc="{COLLABORA_URL}/hosting/discovery#word_edit" />
            <action name="view" ext="doc" urlsrc="{COLLABORA_URL}/hosting/discovery#word_view" />
            <action name="edit" ext="doc" urlsrc="{COLLABORA_URL}/hosting/discovery#word_edit" />
            <action name="view" ext="odt" urlsrc="{COLLABORA_URL}/hosting/discovery#word_view" />
            <action name="edit" ext="odt" urlsrc="{COLLABORA_URL}/hosting/discovery#word_edit" />
        </app>
        <app name="Excel" favIconUrl="{COLLABORA_URL}/favicon.ico">
            <action name="view" ext="xlsx" urlsrc="{COLLABORA_URL}/hosting/discovery#excel_view" />
            <action name="edit" ext="xlsx" urlsrc="{COLLABORA_URL}/hosting/discovery#excel_edit" />
            <action name="view" ext="xls" urlsrc="{COLLABORA_URL}/hosting/discovery#excel_view" />
            <action name="edit" ext="xls" urlsrc="{COLLABORA_URL}/hosting/discovery#excel_edit" />
            <action name="view" ext="csv" urlsrc="{COLLABORA_URL}/hosting/discovery#excel_view" />
            <action name="edit" ext="csv" urlsrc="{COLLABORA_URL}/hosting/discovery#excel_edit" />
            <action name="view" ext="ods" urlsrc="{COLLABORA_URL}/hosting/discovery#excel_view" />
            <action name="edit" ext="ods" urlsrc="{COLLABORA_URL}/hosting/discovery#excel_edit" />
        </app>
    </net-zone>
</wopi-discovery>'''
    
    response = HttpResponse(discovery_xml, content_type='application/xml')
    return response


@require_http_methods(["GET"])
def close_document(request):
    """
    关闭文档后的回调
    """
    return HttpResponse(status=200)


@require_http_methods(["GET"])
def download_document(request, doc_id):
    """
    下载文档
    """
    document = get_document_or_404(doc_id)
    
    file_path = document.get_absolute_path()
    if not os.path.exists(file_path):
        from django.http import Http404
        raise Http404("File not found")
    
    response = FileResponse(
        open(file_path, 'rb'),
        content_type=document.mime_type
    )
    response['Content-Disposition'] = f'attachment; filename="{document.filename}"'
    
    return response


@require_http_methods(["GET"])
def list_documents(request):
    """
    列出所有文档
    前端可以通过此API获取可用的文档列表
    """
    try:
        documents = Document.objects.all().values(
            'id', 'filename', 'document_type', 'file_size', 
            'owner', 'created_at', 'updated_at', 'is_locked'
        )
        
        # 将QuerySet转换为列表
        documents_list = []
        for doc in documents:
            # 格式化日期时间字段
            doc['created_at'] = doc['created_at'].isoformat() if doc['created_at'] else None
            doc['updated_at'] = doc['updated_at'].isoformat() if doc['updated_at'] else None
            documents_list.append(doc)
        
        return JsonResponse({
            'success': True,
            'documents': documents_list,
            'count': len(documents_list)
        })
    except Exception as e:
        return JsonResponse({
            'success': False,
            'error': str(e)
        }, status=500)


@require_http_methods(["POST"])
def create_document(request):
    """
    创建新文档
    前端可以通过此API创建新文档记录
    同时会创建一个空白的实际文件
    """
    import json
    
    try:
        data = json.loads(request.body)
        filename = data.get('filename', '').strip()
        document_type = data.get('document_type', 'word')
        
        if not filename:
            return JsonResponse({
                'success': False,
                'error': 'Filename is required'
            }, status=400)
        
        # 检查文档类型是否有效
        valid_types = ['word', 'excel', 'other']
        if document_type not in valid_types:
            document_type = 'other'
        
        # 确保文件名有正确的扩展名
        ext = os.path.splitext(filename)[1].lower()
        
        # 如果没有扩展名，根据文档类型自动添加
        if not ext:
            if document_type == 'word':
                filename = filename + '.docx'
                ext = '.docx'
            elif document_type == 'excel':
                filename = filename + '.xlsx'
                ext = '.xlsx'
        
        # 构建文件路径
        file_path = f'documents/{filename}'
        absolute_path = os.path.join(settings.MEDIA_ROOT, file_path)
        
        # 确保目录存在
        os.makedirs(os.path.dirname(absolute_path), exist_ok=True)
        
        # 根据文件扩展名创建空白文件
        
        if ext == '.docx' or ext == '.doc':
            # 创建空白Word文档（需要python-docx库）
            # 这里先创建空文件，实际应该使用模板
            blank_content = b''
            try:
                from docx import Document as DocxDocument
                from docx.shared import Inches
                
                # 创建一个最小的空白Word文档
                doc = DocxDocument()
                doc.add_paragraph('')  # 添加空段落
                doc.save(absolute_path)
                blank_content = open(absolute_path, 'rb').read()
            except ImportError:
                # 如果没有python-docx，创建空文件
                with open(absolute_path, 'wb') as f:
                    f.write(b'')
        elif ext == '.xlsx' or ext == '.xls':
            # 创建空白Excel文档（需要openpyxl库）
            try:
                from openpyxl import Workbook
                wb = Workbook()
                wb.save(absolute_path)
            except ImportError:
                with open(absolute_path, 'wb') as f:
                    f.write(b'')
        else:
            # 其他类型创建空文件
            with open(absolute_path, 'wb') as f:
                f.write(b'')
        
        # 获取文件大小
        file_size = os.path.getsize(absolute_path)
        
        # 创建文档记录
        document = Document.objects.create(
            filename=filename,
            document_type=document_type,
            file_path=file_path,
            file_size=file_size,
            owner=data.get('owner', 'admin'),
            is_locked=False
        )
        
        return JsonResponse({
            'success': True,
            'document': {
                'id': str(document.id),
                'filename': document.filename,
                'document_type': document.document_type,
                'file_path': document.file_path,
                'file_size': document.file_size,
                'owner': document.owner,
                'created_at': document.created_at.isoformat(),
                'updated_at': document.updated_at.isoformat(),
                'is_locked': document.is_locked
            }
        })
    except Exception as e:
        return JsonResponse({
            'success': False,
            'error': str(e)
        }, status=500)

# 为开发环境添加CSRF豁免（WOPI API开发常用做法）
# 生产环境建议使用JWT或其他认证方式
csrf_exempt_create_document = csrf_exempt(create_document)