"""
MySQL数据库配置文件
用于配置MySQL数据库连接参数
"""

import os

# MySQL数据库配置
MYSQL_CONFIG = {
    'default': {
        'ENGINE': 'django.db.backends.mysql',
        'NAME': os.environ.get('DB_NAME', 'docs_online'),
        'USER': os.environ.get('DB_USER', 'root'),
        'PASSWORD': os.environ.get('DB_PASSWORD', '*'),  # 请在生产环境中使用强密码
        'HOST': os.environ.get('DB_HOST', 'localhost'),
        'PORT': os.environ.get('DB_PORT', '3306'),
        'OPTIONS': {
            'init_command': "SET sql_mode='STRICT_TRANS_TABLES'",
            'charset': 'utf8mb4',
        },
        'TEST': {
            'CHARSET': 'utf8mb4',
            'COLLATION': 'utf8mb4_unicode_ci',
        }
    }
}

# 环境变量设置示例
ENV_EXAMPLE = """
# .env 文件示例
DB_HOST=localhost
DB_PORT=3306
DB_NAME=docs_online
DB_USER=docs_online_user
DB_PASSWORD=secure_password_for_prod
"""

def get_mysql_config():
    """
    获取MySQL数据库配置
    """
    return MYSQL_CONFIG['default']

def print_env_example():
    """
    打印环境变量配置示例
    """
    print("环境变量配置示例:")
    print(ENV_EXAMPLE)

if __name__ == "__main__":
    print("MySQL配置信息:")
    config = get_mysql_config()
    for key, value in config.items():
        if key != 'PASSWORD':  # 不打印密码
            print(f"{key}: {value}")
        else:
            print(f"{key}: {'*' * len(str(value))}")  # 隐藏密码