import 'package:flutter/material.dart';

void main() => runApp(const StudyOS());

class StudyOS extends StatefulWidget {
  const StudyOS({super.key});
  @override State<StudyOS> createState() => _StudyOSState();
}

class _StudyOSState extends State<StudyOS> {
  ThemeMode mode = ThemeMode.dark;
  @override Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'Study OS', themeMode: mode,
    theme: ThemeData(useMaterial3: true, colorSchemeSeed: const Color(0xFF625BFF), brightness: Brightness.light),
    darkTheme: ThemeData(useMaterial3: true, colorSchemeSeed: const Color(0xFF817AFF), brightness: Brightness.dark, scaffoldBackgroundColor: const Color(0xFF0D0F17)),
    home: Home(onTheme: () => setState(() => mode = mode == ThemeMode.dark ? ThemeMode.light : ThemeMode.dark)),
  );
}

class Home extends StatelessWidget {
  const Home({super.key, required this.onTheme});
  final VoidCallback onTheme;
  @override Widget build(BuildContext context) => Scaffold(
    body: SafeArea(child: ListView(padding: const EdgeInsets.all(20), children: [
      Row(children: [Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('Good evening', style: Theme.of(context).textTheme.titleMedium),
        Text('Ready to focus?', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900)),
      ])), IconButton(onPressed: onTheme, icon: const Icon(Icons.brightness_6_rounded)), const CircleAvatar(child: Icon(Icons.person_rounded))]),
      const SizedBox(height: 24),
      Container(padding: const EdgeInsets.all(22), decoration: BoxDecoration(gradient: LinearGradient(colors: [Theme.of(context).colorScheme.primary, Theme.of(context).colorScheme.secondary]), borderRadius: BorderRadius.circular(28)), child: const Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text("TODAY'S FOCUS", style: TextStyle(color: Colors.white70, letterSpacing: 1.5)), SizedBox(height: 8), Text('0 min', style: TextStyle(color: Colors.white, fontSize: 38, fontWeight: FontWeight.w900)), SizedBox(height: 14), LinearProgressIndicator(value: 0, minHeight: 8, backgroundColor: Colors.white24), SizedBox(height: 10), Text('Start your first study session.', style: TextStyle(color: Colors.white70))])),
      const SizedBox(height: 28), Text('Choose your mode', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)), const SizedBox(height: 12),
      ModeCard(icon: Icons.menu_book_rounded, title: 'Regular Study', subtitle: 'Plan subjects, topics and focused sessions.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const Setup()))),
      ModeCard(icon: Icons.auto_awesome_rounded, title: 'Exam Preparation', subtitle: 'Build a structured plan for an upcoming exam.', onTap: () {}),
      ModeCard(icon: Icons.bolt_rounded, title: 'Next Day Exam', subtitle: 'Prioritize what matters most before tomorrow.', onTap: () {}),
      const SizedBox(height: 24), Text('Today', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)), const SizedBox(height: 10),
      const Card(child: Padding(padding: EdgeInsets.all(20), child: Text('No study plan yet. Create one from Regular Study.'))),
    ])),
  );
}

class ModeCard extends StatelessWidget {
  const ModeCard({super.key, required this.icon, required this.title, required this.subtitle, required this.onTap});
  final IconData icon; final String title, subtitle; final VoidCallback onTap;
  @override Widget build(BuildContext context) => Card(margin: const EdgeInsets.only(bottom: 12), child: ListTile(onTap: onTap, contentPadding: const EdgeInsets.all(12), leading: CircleAvatar(radius: 27, child: Icon(icon)), title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)), subtitle: Text(subtitle), trailing: const Icon(Icons.chevron_right_rounded)));
}

class Setup extends StatefulWidget {
  const Setup({super.key});
  @override State<Setup> createState() => _SetupState();
}
class _SetupState extends State<Setup> {
  int total = 120; final List<String> subjects = []; final input = TextEditingController();
  void add() { final s=input.text.trim(); if(s.isNotEmpty&&!subjects.contains(s)) setState(() {subjects.add(s); input.clear();}); }
  @override void dispose(){input.dispose(); super.dispose();}
  @override Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: const Text('Regular Study')), body: ListView(padding: const EdgeInsets.all(20), children: [
    Text('Build your session', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)), const SizedBox(height: 8),
    const Text('Set total time first. Subject allocations can never exceed this limit.'), const SizedBox(height: 24),
    const Text('TOTAL STUDY TIME', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: 1)), const SizedBox(height: 8),
    Card(child: Column(children: [Padding(padding: const EdgeInsets.only(top: 18), child: Text('${total~/60}h ${total%60}m', style: Theme.of(context).textTheme.displaySmall?.copyWith(fontWeight: FontWeight.w900))), Slider(value: total.toDouble(), min: 25, max: 480, divisions: 91, onChanged: (v)=>setState(()=>total=v.round()))])),
    const SizedBox(height: 24), const Text('SUBJECTS', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: 1)), const SizedBox(height: 8),
    Row(children:[Expanded(child:TextField(controller:input,onSubmitted:(_)=>add(),decoration:const InputDecoration(hintText:'e.g. Mathematics',border:OutlineInputBorder()))),const SizedBox(width:8),IconButton.filled(onPressed:add,icon:const Icon(Icons.add))]),
    const SizedBox(height:12), ...subjects.asMap().entries.map((e)=>Card(child:ListTile(leading:CircleAvatar(child:Text('${e.key+1}')),title:Text(e.value),trailing:IconButton(onPressed:()=>setState(()=>subjects.removeAt(e.key)),icon:const Icon(Icons.close))))),
    const SizedBox(height:20), FilledButton.icon(onPressed:subjects.isEmpty?null:()=>Navigator.push(context,MaterialPageRoute(builder:(_)=>Allocation(subjects:subjects,total:total))),icon:const Icon(Icons.arrow_forward),label:const Padding(padding:EdgeInsets.all(14),child:Text('Set time allocation'))
  ]));
}

class Allocation extends StatefulWidget { const Allocation({super.key,required this.subjects,required this.total}); final List<String> subjects; final int total; @override State<Allocation> createState()=>_AllocationState(); }
class _AllocationState extends State<Allocation>{late List<int> values; @override void initState(){super.initState();values=List.filled(widget.subjects.length,0);} int get used=>values.fold(0,(a,b)=>a+b); @override Widget build(BuildContext context)=>Scaffold(appBar:AppBar(title:const Text('Time allocation')),body:ListView(padding:const EdgeInsets.all(20),children:[Card(child:Padding(padding:const EdgeInsets.all(18),child:Row(mainAxisAlignment:MainAxisAlignment.spaceBetween,children:[Text('Allocated: $used min',style:const TextStyle(fontWeight:FontWeight.w800)),Text('Remaining: ${widget.total-used} min',style:const TextStyle(fontWeight:FontWeight.w800))]))),const SizedBox(height:12),...widget.subjects.asMap().entries.map((e)=>Card(child:Column(children:[ListTile(title:Text(e.value,style:const TextStyle(fontWeight:FontWeight.w800)),trailing:Text('${values[e.key]}m')),Slider(value:values[e.key].toDouble(),min:0,max:widget.total.toDouble(),divisions:widget.total,onChanged:(v){final other=used-values[e.key];setState(()=>values[e.key]=v.round().clamp(0,widget.total-other));}}]))),const SizedBox(height:12),FilledButton.icon(onPressed:used==0?null:()=>Navigator.push(context,MaterialPageRoute(builder:(_)=>Focus(subject:widget.subjects[values.indexWhere((v)=>v>0)],minutes:values.firstWhere((v)=>v>0)))),icon:const Icon(Icons.play_arrow),label:const Padding(padding:EdgeInsets.all(14),child:Text('Start study session'))]));}

class Focus extends StatefulWidget{const Focus({super.key,required this.subject,required this.minutes});final String subject;final int minutes;@override State<Focus> createState()=>_FocusState();}
class _FocusState extends State<Focus>{late int seconds;bool running=true;@override void initState(){super.initState();seconds=widget.minutes*60;tick();}void tick()async{while(mounted&&seconds>0){await Future.delayed(const Duration(seconds:1));if(running&&mounted)setState(()=>seconds--);}}String get clock=>'${(seconds~/60).toString().padLeft(2,'0')}:${(seconds%60).toString().padLeft(2,'0')}';@override Widget build(BuildContext context)=>Scaffold(appBar:AppBar(title:const Text('Focus mode')),body:Center(child:Column(mainAxisAlignment:MainAxisAlignment.center,children:[const Text('CURRENT SUBJECT',style:TextStyle(letterSpacing:2,fontWeight:FontWeight.w800)),const SizedBox(height:10),Text(widget.subject,style:Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight:FontWeight.w900)),const SizedBox(height:40),Text(clock,style:Theme.of(context).textTheme.displayLarge?.copyWith(fontWeight:FontWeight.w900)),const SizedBox(height:40),Row(mainAxisAlignment:MainAxisAlignment.center,children:[IconButton.filledTonal(onPressed:()=>setState(()=>running=!running),icon:Icon(running?Icons.pause:Icons.play),iconSize:30),const SizedBox(width:16),FilledButton.icon(onPressed:()=>Navigator.pushReplacement(context,MaterialPageRoute(builder:(_)=>const Break())),icon:const Icon(Icons.check),label:const Text('Finish early'))]),const SizedBox(height:24),const Text('Focus → finish → recharge.')]));}

class Break extends StatelessWidget{const Break({super.key});@override Widget build(BuildContext context)=>Scaffold(body:Center(child:Padding(padding:const EdgeInsets.all(28),child:Column(mainAxisAlignment:MainAxisAlignment.center,children:[const Icon(Icons.self_improvement_rounded,size:70),const SizedBox(height:20),Text('Great work!',style:Theme.of(context).textTheme.displaySmall?.copyWith(fontWeight:FontWeight.w900)),const SizedBox(height:10),const Text('Take 5–10 minutes to recharge.',textAlign:TextAlign.center),const SizedBox(height:24),const Card(child:Padding(padding:EdgeInsets.all(20),child:Column(children:[Text('💧 Drink water'),SizedBox(height:12),Text('🚶 Walk or stretch'),SizedBox(height:12),Text('👀 Rest your eyes'),SizedBox(height:12),Text('🧘 Breathe and reset')])),),const SizedBox(height:24),FilledButton(onPressed:()=>Navigator.pop(context),child:const Text('Back to plan'))]))));}
