import 'package:flutter/material.dart';

import 'screens/home_screen.dart';

void main() {
  runApp(const NoShortsApp());
}

class NoShortsApp extends StatelessWidget {
  const NoShortsApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NoShorts',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepOrange),
      ),
      home: const HomeScreen(),
    );
  }
}
