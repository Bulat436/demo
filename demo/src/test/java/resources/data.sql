-- Очищаем таблицы в правильном порядке (из-за foreign keys)
DELETE FROM users;
DELETE FROM role;

-- Создаем тестовые роли
INSERT INTO role (id, name) VALUES (1, 'ADMIN');
INSERT INTO role (id, name) VALUES (2, 'MANAGER');
INSERT INTO role (id, name) VALUES (3, 'USER');

-- Создаем тестовых пользователей (пароли закодированы BCrypt)
INSERT INTO users (id, username, password, role_id) VALUES 
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTV6UiC', 1),
(2, 'manager', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTV6UiC', 2),
(3, 'user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTV6UiC', 3);