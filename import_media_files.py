#!/usr/bin/env python
"""
将media目录中的文件信息导入到数据库的脚本
此脚本可用于MySQL或SQLite数据库
"""

import os
import sys
import django
from pathlib import Path
import uuid

# 设置Django环境
def setup_django():
    """设置Django环境"""
    sys.path.append(os.path.dirname(os.path.abspath(__file__)))
    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'DocsOnlineTest.settings')
    
    # 初始化Django
    django.setup()

def import_media_files():
    """导入media目录中的文件到数据库"""
    print("开始导入media目录中的文件...")
    
    # 设置Django环境
    setup_django()
    
    # 导入模型
    from wopi.models import Document
    
    # 检查media目录是否存在
    media_dir = os.path.join(os.path.dirname(__file__), 'media')
    if not os.path.exists(media_dir):
        print(f"Media目录不存在: {media_dir}")
        print("创建media目录...")
        os.makedirs(media_dir, exist_ok=True)
        return
    
    print(f"扫描目录: {media_dir}")
    
    # 扫描media目录中的文件
    imported_count = 0
    for root, dirs, files in os.walk(media_dir):
        for file in files:
            file_path = os.path.relpath(os.path.join(root, file),
                                      os.path.dirname(__file__))
            
            # 检查文件是否已在数据库中
            existing_doc = Document.objects.filter(file_path=file_path).first()
            if existing_doc:
                print(f"跳过已存在的文档: {file_path}")
                continue
            
            # 确定文档类型
            file_ext = os.path.splitext(file)[1].lower()
            doc_type = 'other'
            if file_ext in ['.doc', '.docx', '.rtf']:
                doc_type = 'word'
            elif file_ext in ['.xls', '.xlsx', '.csv']:
                doc_type = 'excel'
            elif file_ext in ['.ppt', '.pptx']:
                doc_type = 'powerpoint'
            elif file_ext in ['.pdf']:
                doc_type = 'pdf'
            
            # 获取文件大小
            full_path = os.path.join(root, file)
            file_size = os.path.getsize(full_path) if os.path.exists(full_path) else 0
            
            # 创建文档记录
            doc = Document.objects.create(
                filename=file,
                document_type=doc_type,
                file_path=file_path,
                file_size=file_size,
                owner='admin',
                is_locked=False
            )
            
            print(f"成功导入文档: {doc.filename} (ID: {doc.id}, Type: {doc.document_type})")
            imported_count += 1
    
    print(f"\n导入完成! 共导入 {imported_count} 个新文档")

def main():
    """主函数"""
    print("="*60)
    print("Media文件导入脚本")
    print("="*60)
    
    # 检查是否已设置Django环境
    try:
        import django
        print("Django环境已找到")
    except ImportError:
        print("错误: 未找到Django环境")
        sys.exit(1)
    
    # 执行导入
    import_media_files()
    
    print("="*60)
    print("脚本执行完毕")

if __name__ == "__main__":
    main()