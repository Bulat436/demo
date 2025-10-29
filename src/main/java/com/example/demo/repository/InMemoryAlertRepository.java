package com.example.demo.repository;

import com.example.demo.model.Alert;
import com.example.demo.model.StatusType;
import org.springframework.stereotype.Repository; // Аннотация Spring

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository 
public class InMemoryAlertRepository {

    // Имитируем таблицу в БД с помощью HashMap
    private final Map<Long, Alert> alerts = new HashMap<>();
    
    // Счетчик для генерации ID. AtomicLong - потокобезопасный
    private final AtomicLong counter = new AtomicLong(1);

    // Метод для получения всех инцидентов
    public List<Alert> findAll() {
        // Возвращаем копию списка значений из Map
        return new ArrayList<>(alerts.values());
    }

    // Метод для поиска инцидента по ID
    // Возвращает Optional - контейнер, который может содержать или не содержать объект
    public Optional<Alert> findById(Long id) {
        return Optional.ofNullable(alerts.get(id));
    }

    // Метод для сохранения инцидента (создание или обновление)
    public Alert save(Alert alert) {
        if (alert.getId() == null) {
            // Если у alert нет ID, значит это новый инцидент
            // Генерируем новый ID и устанавливаем его
            alert.setId(counter.getAndIncrement());
        }
        // Сохраняем инцидент в Map (ключ - id, значение - сам объект)
        alerts.put(alert.getId(), alert);
        return alert;
    }

    // Метод для удаления инцидента по ID
    public void deleteById(Long id) {
        alerts.remove(id);
    }

    // Дополнительный метод для поиска по статусу
    public List<Alert> findByStatus(StatusType status) {
        List<Alert> result = new ArrayList<>();
        // Проходим по всем значениям в Map
        for (Alert alert : alerts.values()) {
            // Если статус совпадает, добавляем в результат
            if (alert.getStatus() == status) {
                result.add(alert);
            }
        }
        return result;
    }

    // Метод для очистки хранилища 
    public void clear() {
        alerts.clear();
        counter.set(1); // Сбрасываем счетчик
    }
}