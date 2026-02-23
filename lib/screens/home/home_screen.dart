import 'package:flutter/material.dart';
import 'package:ebookreader/services/book_service.dart';
import 'package:ebookreader/screens/book/book_detail_screen.dart';
import 'package:ebookreader/constants/api_constants.dart';

class HomeScreen extends StatefulWidget {
  final String token;

  const HomeScreen({super.key, required this.token});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with SingleTickerProviderStateMixin {
  final BookService _bookService = BookService();
  final TextEditingController _searchController = TextEditingController();
  
  List<dynamic> _books = [];
  List<dynamic> _filteredBooks = [];
  bool _isLoading = true;
  bool _isSearching = false;
  late AnimationController _animController;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      duration: const Duration(milliseconds: 600),
      vsync: this,
    );
    _loadBooks();
  }

  @override
  void dispose() {
    _animController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadBooks() async {
    setState(() => _isLoading = true);
    try {
      final books = await _bookService.getAllBooks(widget.token);
      setState(() {
        _books = books;
        _filteredBooks = books;
        _isLoading = false;
      });
      _animController.forward();
    } catch (e) {
      setState(() {
        _books = [];
        _filteredBooks = [];
        _isLoading = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.error_outline, color: Colors.white),
                const SizedBox(width: 12),
                Expanded(child: Text('Ошибка загрузки книг: $e')),
              ],
            ),
            backgroundColor: Colors.red.shade600,
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          ),
        );
      }
    }
  }

  Future<void> _searchBooks(String query) async {
    if (query.isEmpty) {
      setState(() {
        _filteredBooks = _books;
        _isSearching = false;
      });
      return;
    }

    setState(() => _isSearching = true);
    try {
      final results = await _bookService.searchBooks(widget.token, query);
      setState(() {
        _filteredBooks = results;
        _isSearching = false;
      });
    } catch (e) {
      setState(() {
        _filteredBooks = _books.where((book) {
          final title = (book['title'] ?? '').toLowerCase();
          final author = (book['author'] ?? '').toLowerCase();
          final q = query.toLowerCase();
          return title.contains(q) || author.contains(q);
        }).toList();
        _isSearching = false;
      });
    }
  }

  void _openBookDetail(int bookId) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => BookDetailScreen(
          token: widget.token,
          bookId: bookId,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0E27),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              const Color(0xFF0A0E27),
              const Color(0xFF1A1F3A),
            ],
          ),
        ),
        child: SafeArea(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              colors: [
                                const Color(0xFF14FFEC).withValues(alpha: 0.2),
                                const Color(0xFF0D7377).withValues(alpha: 0.1),
                              ],
                            ),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Icon(
                            Icons.auto_stories_rounded,
                            color: Color(0xFF14FFEC),
                            size: 26,
                          ),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Ваша библиотека',
                                style: TextStyle(
                                  fontSize: 26,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                  letterSpacing: 0.5,
                                ),
                              ),
                              Text(
                                '${_books.length} книг',
                                style: TextStyle(
                                  fontSize: 13,
                                  color: Colors.white.withValues(alpha: 0.6),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 18),
                    // Search bar
                    Container(
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(16),
                        gradient: LinearGradient(
                          colors: [
                            Colors.white.withValues(alpha: 0.08),
                            Colors.white.withValues(alpha: 0.04),
                          ],
                        ),
                        border: Border.all(
                          color: Colors.white.withValues(alpha: 0.1),
                          width: 1.5,
                        ),
                      ),
                      child: TextField(
                        controller: _searchController,
                        onChanged: _searchBooks,
                        style: const TextStyle(color: Colors.white),
                        decoration: InputDecoration(
                          hintText: 'Поиск книг...',
                          hintStyle: TextStyle(
                            color: Colors.white.withValues(alpha: 0.4),
                          ),
                          prefixIcon: Icon(
                            Icons.search_rounded,
                            color: const Color(0xFF14FFEC).withValues(alpha: 0.7),
                          ),
                          suffixIcon: _searchController.text.isNotEmpty
                              ? IconButton(
                                  icon: Icon(
                                    Icons.clear_rounded,
                                    color: Colors.white.withValues(alpha: 0.5),
                                  ),
                                  onPressed: () {
                                    _searchController.clear();
                                    _searchBooks('');
                                  },
                                )
                              : null,
                          border: InputBorder.none,
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 20,
                            vertical: 14,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),

              // Books Grid
              Expanded(
                child: _isLoading || _isSearching
                    ? Center(
                        child: CircularProgressIndicator(
                          color: const Color(0xFF14FFEC),
                          strokeWidth: 2.5,
                        ),
                      )
                    : _filteredBooks.isEmpty
                        ? _buildEmptyState()
                        : _buildBooksGrid(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: LinearGradient(
                colors: [
                  Colors.white.withValues(alpha: 0.05),
                  Colors.white.withValues(alpha: 0.02),
                ],
              ),
            ),
            child: Icon(
              Icons.search_off_rounded,
              size: 80,
              color: Colors.white.withValues(alpha: 0.3),
            ),
          ),
          const SizedBox(height: 24),
          Text(
            'Книги не найдены',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: Colors.white.withValues(alpha: 0.8),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Попробуйте поискать по другому названию или автору',
            style: TextStyle(
              fontSize: 14,
              color: Colors.white.withValues(alpha: 0.5),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBooksGrid() {
    return GridView.builder(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 20),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        childAspectRatio: 0.50,
        crossAxisSpacing: 14,
        mainAxisSpacing: 18,
      ),
      itemCount: _filteredBooks.length,
      itemBuilder: (context, index) {
        return TweenAnimationBuilder(
          duration: Duration(milliseconds: 300 + (index * 50)),
          tween: Tween<double>(begin: 0, end: 1),
          builder: (context, double value, child) {
            return Transform.scale(
              scale: value,
              child: Opacity(
                opacity: value,
                child: _buildBookCard(_filteredBooks[index]),
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildBookCard(Map<String, dynamic> book) {
    final bookId = book['id'] as int;
    
    return GestureDetector(
      onTap: () => _openBookDetail(bookId),
      child: Hero(
        tag: 'book-$bookId',
        child: Container(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(24),
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                Colors.white.withValues(alpha: 0.05),
                Colors.white.withValues(alpha: 0.02),
              ],
            ),
            border: Border.all(
              color: Colors.white.withValues(alpha: 0.08),
              width: 1.5,
            ),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.3),
                blurRadius: 15,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Cover image
              Expanded(
                flex: 5,
                child: Container(
                  decoration: BoxDecoration(
                    borderRadius: const BorderRadius.vertical(
                      top: Radius.circular(24),
                    ),
                    gradient: LinearGradient(
                      colors: [
                        Colors.white.withValues(alpha: 0.03),
                        Colors.white.withValues(alpha: 0.01),
                      ],
                    ),
                  ),
                  child: ClipRRect(
                    borderRadius: const BorderRadius.vertical(
                      top: Radius.circular(24),
                    ),
                    child: book['coverUrl'] != null && book['coverUrl'].toString().isNotEmpty
                        ? Image.network(
                            ApiConstants.getCoverUrl(book['coverUrl']),
                            width: double.infinity,
                            fit: BoxFit.cover,
                            loadingBuilder: (context, child, loadingProgress) {
                              if (loadingProgress == null) return child;
                              return Container(
                                color: Colors.white.withValues(alpha: 0.03),
                                child: Center(
                                  child: CircularProgressIndicator(
                                    value: loadingProgress.expectedTotalBytes != null
                                        ? loadingProgress.cumulativeBytesLoaded / 
                                          loadingProgress.expectedTotalBytes!
                                        : null,
                                    color: const Color(0xFF14FFEC),
                                    strokeWidth: 2,
                                  ),
                                ),
                              );
                            },
                            errorBuilder: (context, error, stackTrace) {
                              print('❌ Ошибка загрузки обложки: $error');
                              print('📍 URL: ${ApiConstants.getCoverUrl(book['coverUrl'])}');
                              return _buildPlaceholder();
                            },
                          )
                        : _buildPlaceholder(),
                  ),
                ),
              ),

              // Book info
              Expanded(
                flex: 4,
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Flexible(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              book['title'] ?? 'Без названия',
                              style: const TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                                letterSpacing: 0.3,
                                height: 1.2,
                              ),
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                            ),
                            const SizedBox(height: 6),
                            Text(
                              book['author'] ?? 'Неизвестный автор',
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.white.withValues(alpha: 0.6),
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 7,
                        ),
                        decoration: BoxDecoration(
                          gradient: const LinearGradient(
                            colors: [Color(0xFF14FFEC), Color(0xFF0D7377)],
                          ),
                          borderRadius: BorderRadius.circular(10),
                          boxShadow: [
                            BoxShadow(
                              color: const Color(0xFF14FFEC).withValues(alpha: 0.3),
                              blurRadius: 8,
                              offset: const Offset(0, 2),
                            ),
                          ],
                        ),
                        child: const Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              Icons.play_arrow_rounded,
                              size: 16,
                              color: Colors.white,
                            ),
                            SizedBox(width: 5),
                            Text(
                              'Читать',
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.white,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPlaceholder() {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            Colors.white.withValues(alpha: 0.05),
            Colors.white.withValues(alpha: 0.02),
          ],
        ),
      ),
      child: Center(
        child: Icon(
          Icons.book_rounded,
          size: 80,
          color: Colors.white.withValues(alpha: 0.2),
        ),
      ),
    );
  }
}