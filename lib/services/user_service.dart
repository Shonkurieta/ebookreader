import 'dart:convert';
import 'package:http/http.dart' as http;

class UserService {
  final String baseUrl = 'http://192.168.10.5:8080/api';

  // ========================================
  // ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ
  // ========================================

  /// Получить профиль текущего пользователя
  Future<Map<String, dynamic>> getProfile(String token) async {
    try {
      print('=== GET PROFILE REQUEST ===');
      print('URL: $baseUrl/user/profile');
      print('Token length: ${token.length}');
      print('Token (first 50 chars): ${token.length > 50 ? token.substring(0, 50) : token}...');
      
      // Проверка на двойной Bearer
      if (token.startsWith('Bearer ')) {
        print('⚠️ WARNING: Token already contains "Bearer " prefix!');
        token = token.substring(7);
        print('Fixed token (first 50 chars): ${token.length > 50 ? token.substring(0, 50) : token}...');
      }
      
      final response = await http.get(
        Uri.parse('$baseUrl/user/profile'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty) {
          throw Exception('Сервер вернул пустой ответ');
        }
        return json.decode(response.body);
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else if (response.statusCode == 403) {
        throw Exception('Доступ запрещен');
      } else {
        throw Exception('Ошибка загрузки профиля: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in getProfile: $e');
      rethrow;
    }
  }

  /// ✅ ИСПРАВЛЕНО: Обновить никнейм (правильный эндпоинт /user/nickname)
  Future<Map<String, dynamic>> updateNickname(String token, String nickname) async {
  final response = await http.put(
    Uri.parse('$baseUrl/api/user/nickname'),
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $token',
    },
    body: json.encode({'nickname': nickname}),
  );

  if (response.statusCode == 200) {
    return json.decode(response.body); // ✅ Возвращаем весь ответ
  } else {
    throw Exception(json.decode(response.body)['message'] ?? 'Ошибка обновления никнейма');
  }
  }

  /// Alias для совместимости со старым кодом
  Future<Map<String, dynamic>> updateProfile(String token, String newNickname) async {
    return updateNickname(token, newNickname);
  }

  /// ✅ НОВЫЙ МЕТОД: Обновить токен (используется после смены никнейма)
  Future<Map<String, dynamic>> refreshToken(String token) async {
    try {
      print('=== REFRESH TOKEN REQUEST ===');
      print('URL: $baseUrl/auth/refresh');

      final response = await http.post(
        Uri.parse('$baseUrl/auth/refresh'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty) {
          throw Exception('Сервер вернул пустой ответ');
        }
        return json.decode(response.body);
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else {
        throw Exception('Ошибка обновления токена: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in refreshToken: $e');
      rethrow;
    }
  }

  /// Изменить пароль
  Future<void> changePassword(String token, String oldPassword, String newPassword) async {
    try {
      print('=== CHANGE PASSWORD REQUEST ===');
      print('URL: $baseUrl/user/password');

      final response = await http.put(
        Uri.parse('$baseUrl/user/password'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: json.encode({
          'oldPassword': oldPassword,
          'newPassword': newPassword,
        }),
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        return;
      } else if (response.statusCode == 400) {
        if (response.body.isEmpty) {
          throw Exception('Неверный старый пароль');
        }
        final error = json.decode(response.body);
        throw Exception(error['message'] ?? 'Ошибка изменения пароля');
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else if (response.statusCode == 403) {
        throw Exception('Доступ запрещен');
      } else {
        throw Exception('Ошибка сервера: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in changePassword: $e');
      rethrow;
    }
  }

  // ========================================
  // ЗАКЛАДКИ
  // ========================================

  /// Добавить книгу в закладки
  Future<void> addBookmark(String token, int bookId) async {
    try {
      print('=== ADD BOOKMARK REQUEST ===');
      print('URL: $baseUrl/user/books/$bookId/bookmark');

      final response = await http.post(
        Uri.parse('$baseUrl/user/books/$bookId/bookmark'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        return;
      } else if (response.statusCode == 404) {
        throw Exception('Книга не найдена');
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else if (response.statusCode == 403) {
        throw Exception('Доступ запрещен');
      } else {
        throw Exception('Ошибка добавления в закладки: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in addBookmark: $e');
      rethrow;
    }
  }

  /// Удалить книгу из закладок
  Future<void> removeBookmark(String token, int bookId) async {
    try {
      print('=== REMOVE BOOKMARK REQUEST ===');
      print('URL: $baseUrl/user/books/$bookId/bookmark');

      final response = await http.delete(
        Uri.parse('$baseUrl/user/books/$bookId/bookmark'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        return;
      } else if (response.statusCode == 404) {
        throw Exception('Закладка не найдена');
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else {
        throw Exception('Ошибка удаления из закладок: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in removeBookmark: $e');
      rethrow;
    }
  }

  /// Получить все закладки пользователя
  Future<List<dynamic>> getBookmarks(String token) async {
    try {
      print('=== GET BOOKMARKS REQUEST ===');
      print('URL: $baseUrl/user/books/bookmarks');

      final response = await http.get(
        Uri.parse('$baseUrl/user/books/bookmarks'),
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
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else if (response.statusCode == 404) {
        return [];
      } else {
        throw Exception('Ошибка загрузки закладок: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in getBookmarks: $e');
      return [];
    }
  }

  /// Обновить прогресс чтения
  Future<void> updateProgress(String token, int bookId, int chapter) async {
    try {
      print('=== UPDATE PROGRESS REQUEST ===');
      print('URL: $baseUrl/user/books/$bookId/progress');

      final response = await http.put(
        Uri.parse('$baseUrl/user/books/$bookId/progress'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: json.encode({
          'chapter': chapter,
        }),
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode != 200) {
        throw Exception('Ошибка сохранения прогресса: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in updateProgress: $e');
      rethrow;
    }
  }

  /// Получить прогресс чтения книги
  Future<Map<String, dynamic>> getProgress(String token, int bookId) async {
    try {
      print('=== GET PROGRESS REQUEST ===');
      print('URL: $baseUrl/user/books/$bookId/progress');

      final response = await http.get(
        Uri.parse('$baseUrl/user/books/$bookId/progress'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty) {
          return {'currentChapter': 1, 'isBookmarked': false};
        }
        return json.decode(response.body);
      } else {
        return {'currentChapter': 1, 'isBookmarked': false};
      }
    } catch (e) {
      print('Error in getProgress: $e');
      return {'currentChapter': 1, 'isBookmarked': false};
    }
  }

  // ========================================
  // ADMIN ФУНКЦИИ
  // ========================================

  /// Получить всех пользователей (для админа)
  Future<List<dynamic>> getAllUsers(String token) async {
    try {
      print('=== GET ALL USERS REQUEST ===');
      print('URL: $baseUrl/admin/users');

      final response = await http.get(
        Uri.parse('$baseUrl/admin/users'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      print('Status code: ${response.statusCode}');
      print('Response body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty) {
          return [];
        }
        final data = json.decode(response.body);
        if (data is List) {
          return data;
        } else if (data is Map && data.containsKey('users')) {
          return data['users'];
        }
        return [];
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else if (response.statusCode == 403) {
        throw Exception('Доступ запрещен. Требуются права администратора');
      } else {
        throw Exception('Ошибка загрузки пользователей: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in getAllUsers: $e');
      rethrow;
    }
  }

  /// Alias для совместимости
  Future<List<dynamic>> fetchUsers(String token) async {
    return getAllUsers(token);
  }

  /// Удалить пользователя (для админа)
  Future<void> deleteUser(String token, int userId) async {
    try {
      print('=== DELETE USER REQUEST ===');
      print('URL: $baseUrl/admin/users/$userId');

      final response = await http.delete(
        Uri.parse('$baseUrl/admin/users/$userId'),
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
        throw Exception('Пользователь не найден');
      } else if (response.statusCode == 401) {
        throw Exception('Сессия истекла. Войдите заново');
      } else if (response.statusCode == 403) {
        throw Exception('Доступ запрещен');
      } else {
        throw Exception('Ошибка удаления пользователя: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in deleteUser: $e');
      rethrow;
    }
  }
}