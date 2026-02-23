import 'package:shared_preferences/shared_preferences.dart';

/// Сервис для локального хранения данных сессии пользователя.
///
/// Использует [SharedPreferences] для сохранения и получения
/// JWT-токена, роли пользователя и других данных аутентификации
/// между запусками приложения.
class StorageService {
  /// Сохраняет JWT-токен в локальное хранилище.
  Future<void> saveToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('token', token);
  }

  /// Возвращает сохранённый JWT-токен или `null`, если токен отсутствует.
  Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('token');
  }

  /// Сохраняет роль пользователя (например, USER или ADMIN) в локальное хранилище.
  Future<void> saveRole(String role) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('role', role);
  }

  /// Возвращает сохранённую роль пользователя или `null`, если роль не задана.
  Future<String?> getRole() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('role');
  }

  /// Удаляет токен и роль из локального хранилища (выход из системы).
  Future<void> clearToken() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('token');
    await prefs.remove('role');
  }
}

/// Полностью очищает все данные приложения из локального хранилища.
///
/// Используется при необходимости сброса всех настроек и данных пользователя.
Future<void> clearAllData() async {
  try {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    print('✅ All user data cleared');
  } catch (e) {
    print('Error clearing data: $e');
  }
}

/// Удаляет только данные аутентификации из локального хранилища.
///
/// Очищает токен, роль, имя пользователя и email,
/// не затрагивая другие сохранённые настройки.
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
