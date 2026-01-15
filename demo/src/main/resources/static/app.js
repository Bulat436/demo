const API_BASE_URL = 'http://localhost:8080/api/alerts';

// Загрузка всех инцидентов
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
        updateStatistics(alerts);
        
        // Показываем индикатор кеша если загрузка была быстрой
        if (loadTime < 100) {
            showCacheIndicator();
        }
        
        document.getElementById('lastUpdate').textContent = 
            `Последнее обновление: ${new Date().toLocaleTimeString('ru-RU')}`;
            
    } catch (error) {
        console.error('Error:', error);
        showError('Ошибка при загрузке инцидентов');
    } finally {
        showLoading(false);
    }
}

// Обновление статистики
function updateStatistics(alerts) {
    const statNew = alerts.filter(a => a.status === 'NEW').length;
    const statProgress = alerts.filter(a => a.status === 'IN_PROGRESS').length;
    const statResolved = alerts.filter(a => a.status === 'RESOLVED').length;
    const statTotal = alerts.length;
    
    document.getElementById('statNew').textContent = statNew;
    document.getElementById('statProgress').textContent = statProgress;
    document.getElementById('statResolved').textContent = statResolved;
    document.getElementById('statTotal').textContent = statTotal;
    document.getElementById('alertsCount').textContent = statTotal;
    
    // Показать/скрыть сообщение о пустом списке
    const noAlerts = document.getElementById('noAlerts');
    if (alerts.length === 0) {
        noAlerts.style.display = 'block';
    } else {
        noAlerts.style.display = 'none';
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
                        ${alert.filePath ? `
                            <p class="card-text">
                                <strong>Прикрепленный файл:</strong> 
                                <span class="badge bg-info">
                                    <i class="fas fa-paperclip"></i> ${alert.filePath.split('/').pop()}
                                </span>
                            </p>
                        ` : ''}
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
                            <button class="btn btn-secondary btn-sm mb-1" onclick="openUploadModal(${alert.id})">
                                <i class="fas fa-upload"></i> Загрузить файл
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

// Загрузка файла
async function uploadFile(alertId, file) {
    if (!file) {
        showError('Выберите файл для загрузки');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch(`${API_BASE_URL}/${alertId}/upload`, {
            method: 'POST',
            body: formData
        });

        const result = await response.json();
        
        if (response.ok && result.success) {
            showSuccess(result.message);
            loadAlerts();
        } else {
            showError(result.message || 'Ошибка загрузки файла');
        }
    } catch (error) {
        console.error('Error:', error);
        showError('Ошибка при загрузке файла');
    }
}

// Открытие модального окна загрузки файла
function openUploadModal(alertId) {
    document.getElementById('uploadAlertId').value = alertId;
    document.getElementById('fileInput').value = '';
    new bootstrap.Modal(document.getElementById('uploadModal')).show();
}

// Обработка загрузки файла
async function handleFileUpload() {
    const alertId = document.getElementById('uploadAlertId').value;
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];

    if (!file) {
        showError('Выберите файл для загрузки');
        return;
    }

    // Проверка размера файла (макс. 5MB)
    if (file.size > 5 * 1024 * 1024) {
        showError('Файл слишком большой. Максимальный размер: 5MB');
        return;
    }

    // Показываем индикатор загрузки
    const uploadBtn = document.querySelector('#uploadModal .btn-primary');
    const originalText = uploadBtn.innerHTML;
    uploadBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Загрузка...';
    uploadBtn.disabled = true;

    await uploadFile(alertId, file);

    // Восстанавливаем кнопку
    uploadBtn.innerHTML = originalText;
    uploadBtn.disabled = false;
    
    // Закрываем модальное окно
    const modal = bootstrap.Modal.getInstance(document.getElementById('uploadModal'));
    if (modal) {
        modal.hide();
    }
}

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
    document.getElementById('loading').style.display = show ? 'flex' : 'none';
}

function showSuccess(message) {
    showNotification(message, 'success');
}

function showError(message) {
    showNotification(message, 'danger');
}

function showNotification(message, type) {
    const oldNotifications = document.querySelectorAll('.alert.position-fixed');
    oldNotifications.forEach(n => n.remove());
    
    const notification = document.createElement('div');
    notification.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
    notification.style.cssText = 'top: 20px; right: 20px; z-index: 1050; min-width: 300px; max-width: 400px;';
    notification.innerHTML = `
        <div class="d-flex align-items-center">
            <i class="fas ${type === 'success' ? 'fa-check-circle' : type === 'danger' ? 'fa-exclamation-circle' : 'fa-info-circle'} me-2"></i>
            <span>${message}</span>
        </div>
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(notification);
    
    // Автоматическое скрытие через 5 секунд
    setTimeout(() => {
        if (notification.parentNode) {
            notification.remove();
        }
    }, 5000);
}

function showCacheIndicator() {
    const indicator = document.createElement('div');
    indicator.className = 'alert alert-info alert-dismissible fade show position-fixed';
    indicator.style.cssText = 'bottom: 20px; right: 20px; z-index: 1050; min-width: 250px; max-width: 350px;';
    indicator.innerHTML = `
        <div class="d-flex align-items-center">
            <i class="fas fa-bolt me-2"></i>
            <span>Данные загружены из кеша</span>
        </div>
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(indicator);
    
    setTimeout(() => {
        if (indicator.parentNode) {
            indicator.remove();
        }
    }, 2000);
}

// Обновление времени в заголовке
function updateTime() {
    const now = new Date();
    const timeString = now.toLocaleTimeString('ru-RU');
    const currentTimeElement = document.getElementById('currentTime');
    if (currentTimeElement) {
        currentTimeElement.textContent = timeString;
    }
}

// Загрузка инцидентов при старте
document.addEventListener('DOMContentLoaded', function() {
    // Обновляем время каждую секунду
    setInterval(updateTime, 1000);
    updateTime();
    
    // Загружаем инциденты
    loadAlerts();
});