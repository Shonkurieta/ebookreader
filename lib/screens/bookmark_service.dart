import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

class BookmarkService {
  static const _keyBookmarks = 'bookmarks'; // list of bookIds
  static const _keyProgress = 'reading_progress'; // map bookId -> chapterId

  /// Вернёт set id-ов закладок
  static Future<Set<int>> getBookmarks() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_keyBookmarks);
    if (raw == null || raw.isEmpty) return {};
    final List<dynamic> list = jsonDecode(raw);
    return list.map((e) => e as int).toSet();
  }

  static Future<void> addBookmark(int bookId) async {
    final prefs = await SharedPreferences.getInstance();
    final set = await getBookmarks();
    set.add(bookId);
    await prefs.setString(_keyBookmarks, jsonEncode(set.toList()));
  }

  static Future<void> removeBookmark(int bookId) async {
    final prefs = await SharedPreferences.getInstance();
    final set = await getBookmarks();
    set.remove(bookId);
    await prefs.setString(_keyBookmarks, jsonEncode(set.toList()));
  }

  static Future<bool> isBookmarked(int bookId) async {
    final set = await getBookmarks();
    return set.contains(bookId);
  }

  /// Прогресс: сохраняет последнюю открытую главу (id) для книги
  static Future<void> setLastReadChapter(int bookId, int chapterId) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_keyProgress);
    Map<String, dynamic> map = {};
    if (raw != null && raw.isNotEmpty) map = jsonDecode(raw);
    map['$bookId'] = chapterId;
    await prefs.setString(_keyProgress, jsonEncode(map));
  }

  static Future<int?> getLastReadChapter(int bookId) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_keyProgress);
    if (raw == null || raw.isEmpty) return null;
    final map = jsonDecode(raw) as Map<String, dynamic>;
    final v = map['$bookId'];
    if (v == null) return null;
    return (v is int) ? v : int.tryParse(v.toString());
  }

  static Future<void> clearAll() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keyBookmarks);
    await prefs.remove(_keyProgress);
  }
}
