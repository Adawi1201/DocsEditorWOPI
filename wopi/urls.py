"""
WOPI URL 路由配置

根据 WOPI 规范，所有文件操作都通过同一个 URL（/wopi/files/<id>），
使用不同的 HTTP 方法和 X-WOPI-Override 头来区分操作类型：
- GET: CheckFileInfo
- POST + X-WOPI-Override: LOCK: Lock
- POST + X-WOPI-Override: UNLOCK: Unlock
- POST + X-WOPI-Override: PUT: PutFile
- GET /contents: GetFile
"""

from django.urls import path
from . import views

app_name = 'wopi'

urlpatterns = [
    # WOPI Discovery
    path('discovery', views.wopi_discovery, name='discovery'),
    
    # 文件内容接口 - GET: GetFile（使用 str 转换器）
    path('files/<str:doc_id>/contents', views.GetFile.as_view(), name='get_file'),
    
    # 主文件操作接口 - 所有 WOPI 操作都通过此路径
    # GET: CheckFileInfo
    # POST + X-WOPI-Override: LOCK: Lock
    # POST + X-WOPI-Override: UNLOCK: Unlock  
    # POST + X-WOPI-Override: PUT: PutFile
    path('files/<str:doc_id>', views.CheckFileInfo.as_view(), name='check_file_info'),
    
    # 关闭和下载
    path('close', views.close_document, name='close'),
    path('download/<str:doc_id>', views.download_document, name='download'),
    
    # 文档管理 API
    path('documents/', views.list_documents, name='list_documents'),
    path('documents/create/', views.csrf_exempt_create_document, name='create_document'),
]
