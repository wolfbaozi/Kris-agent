ALTER TABLE users
    ADD COLUMN max_mcp_count INT DEFAULT 10 COMMENT '最大MCP配置数',
    ADD COLUMN max_concurrent_mcp INT DEFAULT 5 COMMENT '最大同时运行MCP数';
