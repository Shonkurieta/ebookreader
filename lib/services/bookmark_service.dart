import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:ebookreader/constants/api_constants.dart';

class BookmarkService {
  // Убрали /api, так как он уже есть в ApiConstants.baseUrl
  
  Future<void> addBookmark(String token, int bookId) async {
    try {
      print('=== ADD BOOKMARK ===');
      print('URL: ${ApiConstants.baseUrl}/user/books/$bookId/bookmark');
      
      final response = await http.post(
        Uri.parse('${ApiConstants.baseUrl}/user/books/$bookId/bookmark'),  // Убрали /api
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
      );

      print('Status: ${response.statusCode}');
      print('Body: ${response.body}');

      if (response.statusCode != 200) {
        throw Exception('Ошибка добавления в закладки: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in addBookmark: $e');
      rethrow;
    }
  }

  Future<void> removeBookmark(String token, int bookId) async {
    try {
      print('=== REMOVE BOOKMARK ===');
      print('URL: ${ApiConstants.baseUrl}/user/books/$bookId/bookmark');
      
      final response = await http.delete(
        Uri.parse('${ApiConstants.baseUrl}/user/books/$bookId/bookmark'),  // Убрали /api
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
      );

      print('Status: ${response.statusCode}');
      print('Body: ${response.body}');

      if (response.statusCode != 200) {
        throw Exception('Ошибка удаления из закладок: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in removeBookmark: $e');
      rethrow;
    }
  }

  Future<List<dynamic>> getBookmarks(String token) async {
    try {
      print('=== GET BOOKMARKS ===');
      print('URL: ${ApiConstants.baseUrl}/user/books/bookmarks');
      
      final response = await http.get(
        Uri.parse('${ApiConstants.baseUrl}/user/books/bookmarks'),  // Убрали /api
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
      );

      print('Status: ${response.statusCode}');
      print('Body: ${response.body}');

      if (response.statusCode == 200) {
        if (response.body.isEmpty || response.body == '[]') {
          return [];
        }
        return json.decode(response.body);
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

  Future<void> updateProgress(String token, int bookId, int chapter) async {
    try {
      print('=== UPDATE PROGRESS ===');
      print('URL: ${ApiConstants.baseUrl}/user/books/$bookId/progress');
      print('Chapter: $chapter');
      
      final response = await http.put(
        Uri.parse('${ApiConstants.baseUrl}/user/books/$bookId/progress'),  // Убрали /api
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: json.encode({'chapter': chapter}),
      );

      print('Status: ${response.statusCode}');
      print('Body: ${response.body}');

      if (response.statusCode != 200) {
        throw Exception('Ошибка сохранения прогресса: ${response.statusCode}');
      }
    } catch (e) {
      print('Error in updateProgress: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> getProgress(String token, int bookId) async {
    try {
      print('=== GET PROGRESS ===');
      print('URL: ${ApiConstants.baseUrl}/user/books/$bookId/progress');
      
      final response = await http.get(
        Uri.parse('${ApiConstants.baseUrl}/user/books/$bookId/progress'),  // Убрали /api
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
      );

      print('Status: ${response.statusCode}');
      print('Body: ${response.body}');

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
}