import 'package:flutter/material.dart';

import '../services/blocker_channel.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  bool? _serviceEnabled;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refresh();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  // 설정 화면에서 돌아왔을 때 상태를 다시 확인한다.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _refresh();
  }

  Future<void> _refresh() async {
    final enabled = await BlockerChannel.isServiceEnabled();
    if (mounted) setState(() => _serviceEnabled = enabled);
  }

  @override
  Widget build(BuildContext context) {
    final enabled = _serviceEnabled;
    return Scaffold(
      appBar: AppBar(title: const Text('NoShorts')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Icon(
              enabled == true ? Icons.shield : Icons.shield_outlined,
              size: 96,
              color: enabled == true ? Colors.green : Colors.grey,
            ),
            const SizedBox(height: 16),
            Text(
              enabled == null
                  ? '상태 확인 중...'
                  : enabled
                      ? '숏폼 차단이 켜져 있어요'
                      : '숏폼 차단이 꺼져 있어요',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 32),
            if (enabled == false)
              FilledButton(
                onPressed: BlockerChannel.openAccessibilitySettings,
                child: const Text('접근성 설정에서 켜기'),
              ),
          ],
        ),
      ),
    );
  }
}
