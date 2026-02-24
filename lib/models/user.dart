/// Модель данных пользователя.
///
/// Используется для представления зарегистрированного пользователя системы.
/// Содержит идентификатор, имя пользователя, адрес электронной почты
/// и роль (например, USER или ADMIN).
class User {
  final int id;
  final String username;
  final String email;
  final String role;

  User({
    required this.id,
    required this.username,
    required this.email,
    required this.role,
  });

  /// Создаёт экземпляр [User] из JSON-объекта, полученного от API.
  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'],
      username: json['username'],
      email: json['email'],
      role: json['role'],
    );
  }
}
