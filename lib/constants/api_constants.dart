import 'package:flutter_dotenv/flutter_dotenv.dart';

class ApiConstants {
  static String get baseUrl => '${dotenv.env['API_BASE_URL']}/api';
  static String get authUrl => '$baseUrl/auth';
  static String get adminUrl => '$baseUrl/admin';
  static String get booksUrl => '$baseUrl/books';
  
  // Добавляем метод для получения URL обложки
  static String getCoverUrl(String coverPath) {
    final apiBase = dotenv.env['API_BASE_URL'] ?? 'http://192.168.10.5:8080';
    
    // Если coverPath уже начинается с /, просто добавляем baseUrl
    if (coverPath.startsWith('/')) {
      return '$apiBase$coverPath';
    }
    // Если нет, добавляем /
    return '$apiBase/$coverPath';
  }
}