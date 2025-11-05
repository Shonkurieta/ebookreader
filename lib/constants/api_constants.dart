import 'package:flutter_dotenv/flutter_dotenv.dart';

class ApiConstants {
  static String get baseUrl => '${dotenv.env['API_BASE_URL']}/api';
  static String get authUrl => '$baseUrl/auth';
  static String get adminUrl => '$baseUrl/admin';
  static String get booksUrl => '$baseUrl/books';
}
