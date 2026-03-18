"""临时调试 URL - 用于测试 /contents 路由"""

from django.urls import path
from django.http import HttpResponse
import logging

logger = logging.getLogger(__name__)

def debug_contents(request, doc_id):
    """调试用的 contents 视图"""
    logger.error(f'DEBUG: Contents called with doc_id={doc_id}')
    logger.error(f'DEBUG: path_info={request.path}')
    return HttpResponse(f'DEBUG: Contents endpoint reached for {doc_id}', content_type='text/plain')

def debug_files(request, doc_id):
    """调试用的 files 视图"""
    logger.error(f'DEBUG: Files called with doc_id={doc_id}')
    return HttpResponse(f'DEBUG: Files endpoint reached for {doc_id}', content_type='text/plain')

# 这些是测试路由，实际项目中应该删除