"""
解锁所有文档 - 用于修复锁定状态问题
"""
import os
import django

# 设置 Django 环境
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'DocsOnlineTest.settings')
django.setup()

from wopi.models import Document

# 解锁所有文档
count = Document.objects.update(is_locked=False, lock_token=None)
print(f"已解锁 {count} 个文档")
