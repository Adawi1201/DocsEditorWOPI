import os
import uuid
from django.db import models
from django.conf import settings


class Document(models.Model):
    """文档模型 - 用于存储可在线编辑的文档"""
    
    DOCUMENT_TYPE_CHOICES = [
        ('word', 'Word 文档'),
        ('excel', 'Excel 工作表'),
        ('other', '其他'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    filename = models.CharField(max_length=255, verbose_name="文件名")
    document_type = models.CharField(
        max_length=20, 
        choices=DOCUMENT_TYPE_CHOICES, 
        default='word',
        verbose_name="文档类型"
    )
    file_path = models.CharField(max_length=500, verbose_name="文件路径")
    file_size = models.BigIntegerField(default=0, verbose_name="文件大小(字节)")
    version = models.IntegerField(default=1, verbose_name="版本号")
    owner = models.CharField(max_length=100, default='admin', verbose_name="所有者")
    created_at = models.DateTimeField(auto_now_add=True, verbose_name="创建时间")
    updated_at = models.DateTimeField(auto_now=True, verbose_name="更新时间")
    is_locked = models.BooleanField(default=False, verbose_name="是否锁定")
    lock_token = models.CharField(max_length=100, blank=True, null=True, verbose_name="锁定令牌")
    
    class Meta:
        db_table = 'wopi_document'
        verbose_name = '文档'
        verbose_name_plural = '文档'
        ordering = ['-updated_at']
    
    def __str__(self):
        return self.filename
    
    @property
    def extension(self):
        """获取文件扩展名"""
        return os.path.splitext(self.filename)[1].lower()
    
    @property
    def mime_type(self):
        """获取 MIME 类型"""
        mime_types = {
            '.docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            '.doc': 'application/msword',
            '.xlsx': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            '.xls': 'application/vnd.ms-excel',
            '.pptx': 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
            '.ppt': 'application/vnd.ms-powerpoint',
            '.odt': 'application/vnd.oasis.opendocument.text',
            '.ods': 'application/vnd.oasis.opendocument.spreadsheet',
            '.odp': 'application/vnd.oasis.opendocument.presentation',
        }
        return mime_types.get(self.extension, 'application/octet-stream')
    
    @property
    def wopi_src(self):
        """生成 WOPI source URL"""
        return f'/wopi/files/{self.id}'
    
    def get_absolute_path(self):
        """获取文件的绝对路径"""
        # 标准化路径：统一使用正斜杠
        file_path = self.file_path.replace('\\', '/')
        
        # 移除可能存在的 media/ 前缀，避免路径重复
        if file_path.startswith('media/'):
            file_path = file_path[6:]  # 去掉 'media/' 前缀
        elif file_path.startswith('/media/'):
            file_path = file_path[7:]  # 去掉 '/media/' 前缀
        
        # 确保路径不以 / 开头
        file_path = file_path.lstrip('/')
        
        return os.path.join(settings.MEDIA_ROOT, file_path)
    
    def increment_version(self):
        """递增版本号"""
        self.version += 1
        self.save(update_fields=['version', 'updated_at'])


# settings.py 中需要添加 MEDIA_ROOT 配置
# MEDIA_ROOT = os.path.join(BASE_DIR, 'media')
# MEDIA_URL = '/media/'