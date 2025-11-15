const API_BASE_URL = 'http://localhost:8080/api/alerts';

// Загрузка всех инцидентов
async function loadAlerts() {
    showLoading(true);
    
    try {
        const statusFilter = document.getElementById('statusFilter').value;
        let url = API_BASE_URL;
        if (statusFilter) {
            url += `?status=${statusFilter}`;
        }

        const response = await fetch(url);
        if (!response.ok) throw new Error('Ошибка загрузки данных');
        
        const alerts = await response.json();
        displayAlerts(alerts);
    } catch (error) {
        console.error('Error:', error);
        showError('Ошибка при загрузке инцидентов');
    } finally {
        showLoading(false);
    }
}

// Отображение инцидентов в таблице
function displayAlerts(alerts) {
    const container = document.getElementById('alertsTable');
    
    if (alerts.length === 0) {
        container.innerHTML = `
            <div class="alert alert-info text-center">
                <i class="fas fa-info-circle"></i> Инциденты не найдены
            </div>
        `;
        return;
    }

    const alertsHtml = alerts.map(alert => `
        <div class="card mb-3 alert-card status-${alert.status}">
            <div class="card-body">
                <div class="row">
                    <div class="col-md-8">
                        <h5 class="card-title">
                            <span class="badge bg-${getStatusBadgeColor(alert.status)}">${getStatusText(alert.status)}</span>
                            Инцидент #${alert.id}
                        </h5>
                        <p class="card-text">
                            <strong>Автобус:</strong> ${alert.busId} | 
                            <strong>Тип:</strong> ${getTypeText(alert.type)} |
                            <strong>Местоположение:</strong> ${alert.location}
                        </p>
                        <p class="card-text">${alert.description}</p>
                        <p class="card-text">
                            <small class="text-muted">
                                <i class="fas fa-clock"></i> ${formatDateTime(alert.timestamp)}
                                ${alert.assignedToUserId ? `| <i class="fas fa-user"></i> Назначен на: ${alert.assignedToUserId}` : ''}
                            </small>
                        </p>
                    </div>
                    <div class="col-md-4 text-end">
                        <div class="btn-group-vertical">
                            ${alert.status !== 'IN_PROGRESS' ? `
                                <button class="btn btn-warning btn-sm mb-1" onclick="updateStatus(${alert.id}, 'IN_PROGRESS')">
                                    <i class="fas fa-play"></i> В работу
                                </button>
                            ` : ''}
                            ${alert.status !== 'RESOLVED' ? `
                                <button class="btn btn-success btn-sm mb-1" onclick="updateStatus(${alert.id}, 'RESOLVED')">
                                    <i class="fas fa-check"></i> Решен
                                </button>
                            ` : ''}
                            <button class="btn btn-info btn-sm mb-1" onclick="openAssignModal(${alert.id})">
                                <i class="fas fa-user-plus"></i> Назначить
                            </button>
                            <button class="btn btn-danger btn-sm" onclick="deleteAlert(${alert.id})">
                                <i class="fas fa-trash"></i> Удалить
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `).join('');

    container.innerHTML = alertsHtml;
}

// Создание нового инцидента
document.getElementById('alertForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const alertData = {
        busId: parseInt(document.getElementById('busId').value),
        type: document.getElementById('type').value,
        location: document.getElementById('location').value,
        description: document.getElementById('description').value
    };

    try {
        const response = await fetch(API_BASE_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(alertData)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(JSON.stringify(errorData));
        }

        const newAlert = await response.json();
        showSuccess('Инцидент успешно создан!');
        document.getElementById('alertForm').reset();
        loadAlerts();
    } catch (error) {
        console.error('Error:', error);
        showError('Ошибка при создании инцидента: ' + error.message);
    }
});

// Обновление статуса инцидента
async function updateStatus(alertId, newStatus) {
    try {
        const response = await fetch(`${API_BASE_URL}/${alertId}/status?status=${newStatus}`, {
            method: 'PUT'
        });

        if (!response.ok) throw new Error('Ошибка обновления статуса');
        
        showSuccess('Статус обновлен!');
        loadAlerts();
    } catch (error) {
        console.error('Error:', error);
        showError('Ошибка при обновлении статуса');
    }
}

// Открытие модального окна для назначения
function openAssignModal(alertId) {
    document.getElementById('assignAlertId').value = alertId;
    document.getElementById('userId').value = '';
    new bootstrap.Modal(document.getElementById('assignModal')).show();
}

// Назначение инцидента пользователю
async function assignAlert() {
    const alertId = document.getElementById('assignAlertId').value;
    const userId = document.getElementById('userId').value;

    if (!userId) {
        showError('Введите ID пользователя');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/${alertId}/assign?userId=${userId}`, {
            method: 'PUT'
        });

        if (!response.ok) throw new Error('Ошибка назначения инцидента');
        
        showSuccess('Инцидент назначен пользователю!');
        bootstrap.Modal.getInstance(document.getElementById('assignModal')).hide();
        loadAlerts();
    } catch (error) {
        console.error('Error:', error);
        showError('Ошибка при назначении инцидента');
    }
}

// Удаление инцидента
async function deleteAlert(alertId) {
    if (!confirm('Вы уверены, что хотите удалить этот инцидент?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/${alertId}`, {
            method: 'DELETE'
        });

        if (!response.ok) throw new Error('Ошибка удаления инцидента');
        
        showSuccess('Инцидент удален!');
        loadAlerts();
    } catch (error) {
        console.error('Error:', error);
        showError('Ошибка при удалении инцидента');
    }
}

// Вспомогательные функции
function getStatusText(status) {
    const statusMap = {
        'NEW': 'Новый',
        'IN_PROGRESS': 'В работе',
        'RESOLVED': 'Решен'
    };
    return statusMap[status] || status;
}

function getTypeText(type) {
    const typeMap = {
        'ACCIDENT': 'Авария',
        'HARD_BRAKING': 'Резкое торможение',
        'BUTTON': 'Нажатие кнопки'
    };
    return typeMap[type] || type;
}

function getStatusBadgeColor(status) {
    const colorMap = {
        'NEW': 'danger',
        'IN_PROGRESS': 'warning',
        'RESOLVED': 'success'
    };
    return colorMap[status] || 'secondary';
}

function formatDateTime(timestamp) {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleString('ru-RU');
}

function showLoading(show) {
    document.getElementById('loading').style.display = show ? 'block' : 'none';
}

function showSuccess(message) {
    showNotification(message, 'success');
}

function showError(message) {
    showNotification(message, 'danger');
}

function showNotification(message, type) {
    const notification = document.createElement('div');
    notification.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
    notification.style.cssText = 'top: 20px; right: 20px; z-index: 1050; min-width: 300px;';
    notification.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        if (notification.parentNode) {
            notification.remove();
        }
    }, 5000);
}

// Загрузка инцидентов при старте
document.addEventListener('DOMContentLoaded', function() {
    loadAlerts();
});

// Добавляем новые функции для работы с кешем

// Функция для очистки кеша
async function clearCache() {
    try {
        const response = await fetch(`${API_BASE_URL}/cache/clear`, {
            method: 'POST'
        });

        if (response.ok) {
            showSuccess('Кеш успешно очищен!');
            // Перезагружаем данные после очистки кеша
            loadAlerts();
        } else {
            throw new Error('Ошибка очистки кеша');
        }
    } catch (error) {
        console.error('Error:', error);
        showError('Ошибка при очистке кеша');
    }
}

// Добавляем кнопку очистки кеша в HTML
// Обновляем раздел с фильтрами в index.html:
document.querySelector('.col-md-6.text-end').innerHTML += `
    <button class="btn btn-outline-warning ms-2" onclick="clearCache()" title="Очистить кеш">
        <i class="fas fa-broom"></i> Очистить кеш
    </button>
`;

// Добавляем индикатор кеширования
function showCacheIndicator() {
    const indicator = document.createElement('div');
    indicator.className = 'alert alert-info alert-dismissible fade show position-fixed';
    indicator.style.cssText = 'bottom: 20px; right: 20px; z-index: 1050; min-width: 250px;';
    indicator.innerHTML = `
        <i class="fas fa-bolt"></i> Данные загружены из кеша
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(indicator);
    
    setTimeout(() => {
        if (indicator.parentNode) {
            indicator.remove();
        }
    }, 2000);
}

// Модифицируем функцию loadAlerts для отслеживания времени загрузки
async function loadAlerts() {
    showLoading(true);
    const startTime = performance.now();
    
    try {
        const statusFilter = document.getElementById('statusFilter').value;
        let url = API_BASE_URL;
        if (statusFilter) {
            url += `?status=${statusFilter}`;
        }

        const response = await fetch(url);
        if (!response.ok) throw new Error('Ошибка загрузки данных');
        
        const alerts = await response.json();
        const loadTime = performance.now() - startTime;
        
        displayAlerts(alerts);
        
        // Показываем индикатор кеша если загрузка была быстрой
        if (loadTime < 100) {
            showCacheIndicator();
        }
        
    } catch (error) {
        console.error('Error:', error);
        showError('Ошибка при загрузке инцидентов');
    } finally {
        showLoading(false);
    }
}