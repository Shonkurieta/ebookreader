import 'package:shared_preferences/shared_preferences.dart';

class StorageService {
  Future<void> saveToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('token', token);
  }

  Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('token');
  }

  Future<void> saveRole(String role) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('role', role);
  }

  Future<String?> getRole() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('role');
  }

  Future<void> clearToken() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('token');
    await prefs.remove('role');
  }
}

// Добавьте это в StorageService.dart

/// Полная очистка всех данных пользователя
Future<void> clearAllData() async {
  try {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear(); // Удаляет ВСЕ данные
    print('✅ All user data cleared');
  } catch (e) {
    print('Error clearing data: $e');
  }
}

/// Или очистить только auth данные
Future<void> clearAuthData() async {
  try {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('token');
    await prefs.remove('role');
    await prefs.remove('username');
    await prefs.remove('email');
    print('✅ Auth data cleared');
  } catch (e) {
    print('Error clearing auth data: $e');
  }
}