/// Модель данных книги.
///
/// Используется для представления книги, полученной с сервера.
/// Содержит основные метаданные: идентификатор, название, автора,
/// описание, а также ссылки на файл и обложку.
class Book {
  final int id;
  final String title;
  final String author;
  final String description;
  final String fileUrl;
  final String coverUrl;

  Book({
    required this.id,
    required this.title,
    required this.author,
    required this.description,
    required this.fileUrl,
    required this.coverUrl,
  });

  /// Создаёт экземпляр [Book] из JSON-объекта, полученного от API.
  factory Book.fromJson(Map<String, dynamic> json) {
    return Book(
      id: json['id'],
      title: json['title'],
      author: json['author'],
      description: json['description'],
      fileUrl: json['fileUrl'],
      coverUrl: json['coverUrl'],
    );
  }
}
