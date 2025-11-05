import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/book.dart';
import '../constants/api_constants.dart';

class ApiService {
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
