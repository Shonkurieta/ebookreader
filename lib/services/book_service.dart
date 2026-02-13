import 'dart:convert';
import 'package:http/http.dart' as http;

class BookService {
  final String baseUrl = 'http://172.28.59.182:8080/api';

  // Получить все книги
  Future<List<dynamic>> getAllBooks(String token) async {
    try {
      print('=== GET ALL BOOKS REQUEST ===');
      print('URL: $baseUrl/books');

      final response = await http.get(
        Uri.parse('$baseUrl/books'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty || response.body == '[]') {
          return [];
        }
        final data = json.decode(response.body);
        if (data is List) {
          return data;
        } else if (data is Map && data.containsKey('books')) {
          return data['books'];
        }
        return [];
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else if (response.statusCode == 403) {
        throw Exception('Доступ запрещен');
      } else {
        throw Exception('Ошибка загрузки книг: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in getAllBooks: $e');
      rethrow;
    }
  }

  // Получить все книги (для админа)
  Future<List<dynamic>> getAdminBooks(String token) async {
    try {
      print('=== GET ADMIN BOOKS REQUEST ===');
      print('URL: $baseUrl/admin/books');

      final response = await http.get(
        Uri.parse('$baseUrl/admin/books'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty || response.body == '[]') {
          return [];
        }
        final data = json.decode(response.body);
        if (data is List) {
          return data;
        } else if (data is Map && data.containsKey('books')) {
          return data['books'];
        }
        return [];
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else if (response.statusCode == 403) {
        throw Exception('Доступ запрещен. Требуются права администратора');
      } else {
        throw Exception('Ошибка загрузки книг: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in getAdminBooks: $e');
      rethrow;
    }
  }

  // Получить книгу по ID
  Future<Map<String, dynamic>> getBookById(String token, int bookId) async {
    try {
      print('=== GET BOOK BY ID REQUEST ===');
      print('URL: $baseUrl/books/$bookId');

      final response = await http.get(
        Uri.parse('$baseUrl/books/$bookId'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty) {
          throw Exception('Книга не найдена');
        }
        return json.decode(response.body);
      } else if (response.statusCode == 404) {
        throw Exception('Книга не найдена');
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else {
        throw Exception('Ошибка загрузки книги: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in getBookById: $e');
      rethrow;
    }
  }

  // Добавить книгу (для админа)
  Future<Map<String, dynamic>> addBook(String token, Map<String, dynamic> bookData) async {
    try {
      print('=== ADD BOOK REQUEST ===');
      print('URL: $baseUrl/admin/books');
      print('Book data: $bookData');

      final response = await http.post(
        Uri.parse('$baseUrl/admin/books'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: json.encode(bookData),
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200 || response.statusCode == 201) {
        if (response.body.isEmpty) {
          return {'success': true, 'message': 'Книга добавлена'};
        }
        return json.decode(response.body);
      } else if (response.statusCode == 400) {
        final error = json.decode(response.body);
        throw Exception(error['message'] ?? 'Неверные данные книги');
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else if (response.statusCode == 403) {
        throw Exception('Доступ запрещен. Требуются права администратора');
      } else {
        throw Exception('Ошибка добавления книги: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in addBook: $e');
      rethrow;
    }
  }

  // Обновить книгу (для админа)
  Future<Map<String, dynamic>> updateBook(String token, int bookId, Map<String, dynamic> bookData) async {
    try {
      print('=== UPDATE BOOK REQUEST ===');
      print('URL: $baseUrl/admin/books/$bookId');
      print('Book data: $bookData');

      final response = await http.put(
        Uri.parse('$baseUrl/admin/books/$bookId'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: json.encode(bookData),
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty) {
          return {'success': true, 'message': 'Книга обновлена'};
        }
        return json.decode(response.body);
      } else if (response.statusCode == 404) {
        throw Exception('Книга не найдена');
      } else if (response.statusCode == 400) {
        final error = json.decode(response.body);
        throw Exception(error['message'] ?? 'Неверные данные книги');
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else if (response.statusCode == 403) {
        throw Exception('Доступ запрещен');
      } else {
        throw Exception('Ошибка обновления книги: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in updateBook: $e');
      rethrow;
    }
  }

  // Удалить книгу (для админа)
  Future<void> deleteBook(String token, int bookId) async {
    try {
      print('=== DELETE BOOK REQUEST ===');
      print('URL: $baseUrl/admin/books/$bookId');
      print('Token: ${token.substring(0, 20)}...'); // Показываем начало токена

      final response = await http.delete(
        Uri.parse('$baseUrl/admin/books/$bookId'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200 || response.statusCode == 204) {
        return;
      } else if (response.statusCode == 404) {
        throw Exception('Книга не найдена');
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else if (response.statusCode == 403) {
        throw Exception('Доступ запрещен. Требуются права администратора');
      } else {
        throw Exception('Ошибка удаления книги: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in deleteBook: $e');
      rethrow;
    }
  }

  // Alias для совместимости (для обычных пользователей)
  Future<List<dynamic>> fetchBooks(String token) async {
    return getAllBooks(token);
  }

  // Поиск книг
  Future<List<dynamic>> searchBooks(String token, String query) async {
    try {
      print('=== SEARCH BOOKS REQUEST ===');
      print('Query: "$query"');
      print('URL: $baseUrl/books/search?query=$query');

      final response = await http.get(
        Uri.parse('$baseUrl/books/search?query=${Uri.encodeComponent(query)}'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty || response.body == '[]') {
          return [];
        }
        final data = json.decode(response.body);
        if (data is List) {
          print('✅ Found ${data.length} books');
          return data;
        }
        return [];
      } else {
        print('⚠️ Search failed with status: ${response.statusCode}');
        return [];
      }
    } catch (e) {
      print('❌ Error in searchBooks: $e');
      return [];
    }
  }

  // Получить главы книги
  Future<List<dynamic>> getBookChapters(String token, int bookId) async {
    try {
      print('=== GET BOOK CHAPTERS REQUEST ===');
      print('URL: $baseUrl/books/$bookId/chapters');

      final response = await http.get(
        Uri.parse('$baseUrl/books/$bookId/chapters'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty || response.body == '[]') {
          return [];
        }
        final data = json.decode(response.body);
        if (data is List) {
          return data;
        }
        return [];
      } else if (response.statusCode == 404) {
        return [];
      } else {
        throw Exception('Ошибка загрузки глав: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in getBookChapters: $e');
      return [];
    }
  }

  // Получить конкретную главу
  Future<Map<String, dynamic>> getChapter(String token, int bookId, int chapterOrder) async {
    try {
      print('=== GET CHAPTER REQUEST ===');
      print('URL: $baseUrl/books/$bookId/chapters/$chapterOrder');

      final response = await http.get(
        Uri.parse('$baseUrl/books/$bookId/chapters/$chapterOrder'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty) {
          throw Exception('Глава не найдена');
        }
        return json.decode(response.body);
      } else if (response.statusCode == 404) {
        throw Exception('Глава не найдена');
      } else {
        throw Exception('Ошибка загрузки главы: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in getChapter: $e');
      rethrow;
    }
  }
}