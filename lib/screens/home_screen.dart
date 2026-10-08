import 'package:flutter/material.dart';

import '../services/blocker_channel.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  bool? _serviceEnabled;
  BlockerSettings? _settings;

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
    final settings = await BlockerChannel.getSettings();
    if (mounted) {
      setState(() {
        _serviceEnabled = enabled;
        _settings = settings;
      });
    }
  }

  Future<void> _update({bool? maskEnabled, bool? warningEnabled}) async {
    final settings = await BlockerChannel.setSettings(
      maskEnabled: maskEnabled,
      warningEnabled: warningEnabled,
    );
    if (mounted) setState(() => _settings = settings);
  }

  @override
  Widget build(BuildContext context) {
    final enabled = _serviceEnabled;
    final settings = _settings;
    return Scaffold(
      appBar: AppBar(title: const Text('NoShorts')),
      body: ListView(
        padding: const EdgeInsets.all(24),
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
          const SizedBox(height: 24),
          if (enabled == false)
            FilledButton(
              onPressed: BlockerChannel.openAccessibilitySettings,
              child: const Text('접근성 설정에서 켜기'),
            ),
          if (settings != null) ...[
            const SizedBox(height: 16),
            SwitchListTile(
              title: const Text('쇼츠 버튼 가리기'),
              subtitle: const Text('쇼츠·릴스 탭 버튼 위에 가림막을 띄워 누르지 못하게 해요'),
              value: settings.maskEnabled,
              onChanged: (v) => _update(maskEnabled: v),
            ),
            SwitchListTile(
              title: const Text('들어가기 전에 경고하기'),
              subtitle: const Text('숏폼 화면에 들어가면 경고 화면을 먼저 보여줘요'),
              value: settings.warningEnabled,
              onChanged: (v) => _update(warningEnabled: v),
            ),
            if (settings.allowUntil.isAfter(DateTime.now()))
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  '${TimeOfDay.fromDateTime(settings.allowUntil).format(context)}까지 잠깐 허용 중이에요',
                  textAlign: TextAlign.center,
                ),
              ),
          ],
        ],
      ),
    );
  }
}
