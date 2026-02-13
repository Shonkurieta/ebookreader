# Используем официальный образ OpenJDK для сборки
FROM openjdk:17-jdk-slim as builder

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем Maven POM файл и исходный код
COPY pom.xml .
COPY src ./src

# Собираем приложение с помощью Maven. Флаг -Dmaven.test.skip=true пропускает тесты.
# Это создаст исполняемый JAR-файл в target/
RUN ./mvnw clean package -Dmaven.test.skip=true

# Используем более легковесный образ JRE для запуска приложения
FROM openjdk:17-jre-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR-файл из предыдущего этапа сборки
COPY --from=builder /app/target/ebookreader-backend-1.0.0.jar ebookreader-backend.jar

# Открываем порт, на котором работает Spring Boot приложение
EXPOSE 8080

# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "ebookreader-backend.jar"]