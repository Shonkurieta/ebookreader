import 'package:flutter/material.dart';
import 'admin_books_screen.dart';
import 'admin_users_screen.dart';
import 'admin_profile_screen.dart';

/// Главный экран панели администратора.
///
/// Реализует нижнюю навигационную панель с тремя разделами:
/// управление книгами ([AdminBooksScreen]),
/// управление пользователями ([AdminUsersScreen])
/// и профиль администратора ([AdminProfileScreen]).
class AdminMainScreen extends StatefulWidget {
  final String token;

  const AdminMainScreen({super.key, required this.token});

  @override
  State<AdminMainScreen> createState() => _AdminMainScreenState();
}

class _AdminMainScreenState extends State<AdminMainScreen> with SingleTickerProviderStateMixin {
  int _selectedIndex = 0;
  late AnimationController _animController;
  late List<Widget> _screens;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    _screens = [
      AdminBooksScreen(token: widget.token),
      AdminUsersScreen(token: widget.token),
      AdminProfileScreen(token: widget.token),
    ];
    _animController.forward();
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  void _onItemTapped(int index) {
    if (_selectedIndex != index) {
      setState(() => _selectedIndex = index);
      _animController.reset();
      _animController.forward();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0E27),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              const Color(0xFF0A0E27),
              const Color(0xFF1A1F3A),
              const Color(0xFF0D7377).withValues(alpha: 0.3),
            ],
          ),
        ),
        child: FadeTransition(
          opacity: _animController,
          child: _screens[_selectedIndex],
        ),
      ),
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              const Color(0xFF1A1F3A).withValues(alpha: 0.95),
              const Color(0xFF0A0E27),
            ],
          ),
          border: Border(
            top: BorderSide(
              color: const Color(0xFF14FFEC).withValues(alpha: 0.2),
              width: 1.5,
            ),
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.3),
              blurRadius: 20,
              offset: const Offset(0, -5),
            ),
          ],
        ),
        child: SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildNavItem(
                  icon: Icons.auto_stories_rounded,
                  label: 'Книги',
                  index: 0,
                ),
                _buildNavItem(
                  icon: Icons.people_rounded,
                  label: 'Пользователи',
                  index: 1,
                ),
                _buildNavItem(
                  icon: Icons.person_rounded,
                  label: 'Профиль',
                  index: 2,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildNavItem({
    required IconData icon,
    required String label,
    required int index,
  }) {
    final isSelected = _selectedIndex == index;

    return Expanded(
      child: GestureDetector(
        onTap: () => _onItemTapped(index),
        child: TweenAnimationBuilder(
          duration: const Duration(milliseconds: 250),
          tween: Tween<double>(begin: 0, end: isSelected ? 1 : 0),
          curve: Curves.easeInOut,
          builder: (context, double value, child) {
            return Container(
              margin: const EdgeInsets.symmetric(horizontal: 4),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(
                gradient: value > 0
                    ? LinearGradient(
                        colors: [
                          const Color(0xFF14FFEC).withValues(alpha: 0.25 * value),
                          const Color(0xFF0D7377).withValues(alpha: 0.15 * value),
                        ],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      )
                    : null,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(
                  color: isSelected
                      ? const Color(0xFF14FFEC).withValues(alpha: 0.4 * value)
                      : Colors.transparent,
                  width: 1.5,
                ),
                boxShadow: isSelected
                    ? [
                        BoxShadow(
                          color: const Color(0xFF14FFEC).withValues(alpha: 0.3 * value),
                          blurRadius: 12 * value,
                          spreadRadius: 2 * value,
                        ),
                      ]
                    : null,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Иконка с анимацией масштаба
                  Transform.scale(
                    scale: 1 + (0.15 * value),
                    child: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        gradient: value > 0
                            ? LinearGradient(
                                colors: [
                                  const Color(0xFF14FFEC).withValues(alpha: 0.3 * value),
                                  const Color(0xFF0D7377).withValues(alpha: 0.2 * value),
                                ],
                              )
                            : null,
                      ),
                      child: Icon(
                        icon,
                        color: Color.lerp(
                          Colors.white.withValues(alpha: 0.5),
                          const Color(0xFF14FFEC),
                          value,
                        ),
                        size: 24,
                      ),
                    ),
                  ),
                  const SizedBox(height: 6),
                  // Текст с анимацией
                  Text(
                    label,
                    style: TextStyle(
                      color: Color.lerp(
                        Colors.white.withValues(alpha: 0.5),
                        const Color(0xFF14FFEC),
                        value,
                      ),
                      fontSize: 10,
                      fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                      letterSpacing: 0.2,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  // Индикатор активности
                  const SizedBox(height: 4),
                  AnimatedContainer(
                    duration: const Duration(milliseconds: 250),
                    height: 3,
                    width: isSelected ? 20 : 0,
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [Color(0xFF14FFEC), Color(0xFF0D7377)],
                      ),
                      borderRadius: BorderRadius.circular(2),
                      boxShadow: isSelected
                          ? [
                              BoxShadow(
                                color: const Color(0xFF14FFEC).withValues(alpha: 0.5),
                                blurRadius: 6,
                              ),
                            ]
                          : null,
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}