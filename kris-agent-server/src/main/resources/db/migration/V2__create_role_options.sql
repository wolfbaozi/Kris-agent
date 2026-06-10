CREATE TABLE IF NOT EXISTS role_options (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_key VARCHAR(50) NOT NULL UNIQUE,
    role_label VARCHAR(100) NOT NULL,
    role_desc VARCHAR(255) DEFAULT '',
    sort_order INT DEFAULT 0,
    enabled INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO role_options (role_key, role_label, role_desc, sort_order) VALUES
('developer', '开发者', '可以编写代码，配置 Tool 和 Prompt 类型 Skill', 1),
('product_manager', '产品经理', '不写代码，通过自然语言描述由 AI 生成配置', 2),
('designer', '设计师', '不熟悉代码，优先生成 Prompt 类型 Skill', 3);
