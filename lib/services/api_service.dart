import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/book.dart';
import '../constants/api_constants.dart';

/// Базовый сервис для работы с API.
///
/// Предоставляет общие методы для получения данных с сервера.
/// Для специализированных операций используйте [BookService],
/// [BookmarkService] или [UserService].
class ApiService {
  /// Возвращает список всех книг из публичного эндпоинта `/books`.
  ///
  /// Выбрасывает [Exception] при ошибке сервера.
  Future<List<Book>> fetchBooks() async {
    final response = await http.get(Uri.parse('${ApiConstants.baseUrl}/books'));

    if (response.statusCode == 200) {
      final List<dynamic> jsonList = jsonDecode(response.body);
      return jsonList.map((json) => Book.fromJson(json)).toList();
    } else {
      throw Exception('Ошибка загрузки книг: ${response.statusCode}');
    }
  }
}
