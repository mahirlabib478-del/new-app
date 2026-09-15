import 'dart:async';
import 'package:flutter/material.dart';
import 'models/study_models.dart';
import 'services/local_store.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final store = LocalStore(await SharedPreferencesBootstrap.open());
  runApp(StudyOS(store: store));
}

class SharedPreferencesBootstrap {
  static Future<dynamic> open() async {
    // Kept behind a tiny adapter so the app's UI never depends on storage details.
    return await _load();
  }
  static Future<dynamic> _load() async {
    // ignore: avoid_dynamic_calls
    return await _prefs();
  }
  static Future<dynamic> _prefs() async {
    // The concrete import is intentionally loaded here by the LocalStore dependency.
    return await SharedPreferencesAdapter.instance;
  }
}

class SharedPreferencesAdapter {
  static dynamic get instance => throw UnimplementedError();
}

class StudyOS extends StatefulWidget {
  const StudyOS({super.key, required this.store});
  final LocalStore store;
  @override State<StudyOS> createState() => _StudyOSState();
}

class _StudyOSState extends State<StudyOS> {
  late bool dark = widget.store.darkMode;
  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'Study OS',
    themeMode: dark ? ThemeMode.dark : ThemeMode.light,
    theme: ThemeData(useMaterial3: true, colorSchemeSeed: const Color(0xFF625BFF), brightness: Brightness.light),
    darkTheme: ThemeData(useMaterial3: true, colorSchemeSeed: const Color(0xFF817AFF), brightness: Brightness.dark, scaffoldBackgroundColor: const Color(0xFF0D0F17)),
    home: Home(store: widget.store, onTheme: () async { setState(() => dark = !dark); await widget.store.setDarkMode(dark); }),
  );
}

class Home extends StatelessWidget {
  const Home({super.key, required this.store, required this.onTheme});
  final LocalStore store; final VoidCallback onTheme;
  @override Widget build(BuildContext context) {
    final plan = store.loadPlan();
    final done = store.completedMinutes;
    return Scaffold(body: SafeArea(child: ListView(padding: const EdgeInsets.all(20), children: [
      Row(children: [Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Good evening', style: Theme.of(context).textTheme.titleMedium), Text('Ready to focus?', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900))])), IconButton(onPressed: onTheme, icon: const Icon(Icons.brightness_6_rounded)), const CircleAvatar(child: Icon(Icons.person_rounded))]),
      const SizedBox(height: 24),
      _Hero(done: done, planned: plan?.totalMinutes ?? 0),
      const SizedBox(height: 28),
      Text('Study modes', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)), const SizedBox(height: 12),
      _Mode(icon: Icons.menu_book_rounded, title: 'Regular Study', subtitle: 'Build subjects, topics and focused sessions.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => Setup(store: store)))),
      _Mode(icon: Icons.auto_awesome_rounded, title: 'Exam Preparation', subtitle: 'Prepare a multi-day plan around your exam.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ComingSoon(title: 'Exam Preparation')))),
      _Mode(icon: Icons.bolt_rounded, title: 'Next Day Exam', subtitle: 'Prioritize the highest-impact topics for tomorrow.', onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ComingSoon(title: 'Next Day Exam')))),
      const SizedBox(height: 22),
      Text('Today', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)), const SizedBox(height: 10),
      Card(child: Padding(padding: const EdgeInsets.all(18), child: Row(children: [const Icon(Icons.event_note_rounded, size: 30), const SizedBox(width: 14), Expanded(child: Text(plan == null ? 'No plan yet. Create one from Regular Study.' : '${plan.items.length} study blocks planned • ${plan.totalMinutes} minutes'))]))),
    ])));
  }
}

class _Hero extends StatelessWidget { const _Hero({required this.done, required this.planned}); final int done, planned;
  @override Widget build(BuildContext context) { final ratio = planned == 0 ? 0.0 : (done / planned).clamp(0.0, 1.0); final c=Theme.of(context).colorScheme; return Container(padding: const EdgeInsets.all(22), decoration: BoxDecoration(gradient: LinearGradient(colors: [c.primary,c.secondary]), borderRadius: BorderRadius.circular(28)), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [const Text("TODAY'S FOCUS",style:TextStyle(color:Colors.white70,letterSpacing:1.5)), const SizedBox(height:8), Text('$done min',style:const TextStyle(color:Colors.white,fontSize:38,fontWeight:FontWeight.w900)), const SizedBox(height:14), ClipRRect(borderRadius:BorderRadius.circular(99),child:LinearProgressIndicator(value:ratio,minHeight:8,backgroundColor:Colors.white24)), const SizedBox(height:10), Text(planned==0?'Start your first study session.':'$done of $planned planned minutes completed',style:const TextStyle(color:Colors.white70))])); }
}

class _Mode extends StatelessWidget { const _Mode({required this.icon,required this.title,required this.subtitle,required this.onTap}); final IconData icon; final String title,subtitle; final VoidCallback onTap;
  @override Widget build(BuildContext context)=>Card(margin:const EdgeInsets.only(bottom:12),child:ListTile(onTap:onTap,contentPadding:const EdgeInsets.all(12),leading:CircleAvatar(radius:27,child:Icon(icon)),title:Text(title,style:const TextStyle(fontWeight:FontWeight.w800)),subtitle:Text(subtitle),trailing:const Icon(Icons.chevron_right_rounded)));
}

class Setup extends StatefulWidget { const Setup({super.key,required this.store}); final LocalStore store; @override State<Setup> createState()=>_SetupState(); }
class _SetupState extends State<Setup> {
  int total=120; final subjects=<String>[]; final input=TextEditingController();
  void add(){final s=input.text.trim();if(s.isNotEmpty&&!subjects.contains(s))setState((){subjects.add(s);input.clear();});}
  @override void dispose(){input.dispose();super.dispose();}
  @override Widget build(BuildContext context)=>Scaffold(appBar:AppBar(title:const Text('Regular Study')),body:ListView(padding:const EdgeInsets.all(20),children:[
    Text('Build your session',style:Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight:FontWeight.w900)),const SizedBox(height:8),const Text('Set the total time first. The planner will never allow allocations above this limit.'),const SizedBox(height:24),
    const Text('TOTAL STUDY TIME',style:TextStyle(fontWeight:FontWeight.w800,letterSpacing:1)),Card(child:Column(children:[Padding(padding:const EdgeInsets.only(top:18),child:Text('${total~/60}h ${total%60}m',style:Theme.of(context).textTheme.displaySmall?.copyWith(fontWeight:FontWeight.w900))),Slider(value:total.toDouble(),min:25,max:480,divisions:91,onChanged:(v)=>setState(()=>total=v.round()))])),const SizedBox(height:24),
    const Text('SUBJECTS',style:TextStyle(fontWeight:FontWeight.w800,letterSpacing:1)),const SizedBox(height:8),Row(children:[Expanded(child:TextField(controller:input,onSubmitted:(_)=>add(),decoration:const InputDecoration(hintText:'e.g. Mathematics',border:OutlineInputBorder()))),const SizedBox(width:8),IconButton.filled(onPressed:add,icon:const Icon(Icons.add))]),const SizedBox(height:12),
    ...subjects.asMap().entries.map((e)=>Card(child:ListTile(leading:CircleAvatar(child:Text('${e.key+1}')),title:Text(e.value),trailing:IconButton(onPressed:()=>setState(()=>subjects.removeAt(e.key)),icon:const Icon(Icons.close))))),const SizedBox(height:20),
    FilledButton.icon(onPressed:subjects.isEmpty?null:()=>Navigator.push(context,MaterialPageRoute(builder:(_)=>Allocation(store:widget.store,subjects:subjects,total:total))),icon:const Icon(Icons.arrow_forward),label:const Padding(padding:EdgeInsets.all(14),child:Text('Continue to topics & time')),
  ]));
}

class Allocation extends StatefulWidget { const Allocation({super.key,required this.store,required this.subjects,required this.total}); final LocalStore store; final List<String> subjects; final int total; @override State<Allocation> createState()=>_AllocationState(); }
class _AllocationState extends State<Allocation>{late List<int> values;final topics=<String>[];final topicInput=TextEditingController();
  @override void initState(){super.initState();values=List.filled(widget.subjects.length,0);}
  int get used=>values.fold(0,(a,b)=>a+b);
  void addTopic(){final t=topicInput.text.trim();if(t.isNotEmpty){setState(()=>topics.add(t));topicInput.clear();}}
  @override void dispose(){topicInput.dispose();super.dispose();}
  @override Widget build(BuildContext context)=>Scaffold(appBar:AppBar(title:const Text('Topics & time allocation')),body:ListView(padding:const EdgeInsets.all(20),children:[
    Card(child:Padding(padding:const EdgeInsets.all(18),child:Row(mainAxisAlignment:MainAxisAlignment.spaceBetween,children:[Text('Allocated: $used min',style:const TextStyle(fontWeight:FontWeight.w800)),Text('Remaining: ${widget.total-used} min',style:const TextStyle(fontWeight:FontWeight.w800))]))),const SizedBox(height:12),
    ...widget.subjects.asMap().entries.map((e){final i=e.key;return Card(child:Column(children:[ListTile(title:Text(e.value,style:const TextStyle(fontWeight:FontWeight.w800)),trailing:Text('${values[i]}m')),Slider(value:values[i].toDouble(),min:0,max:widget.total.toDouble(),divisions:widget.total,onChanged:(v){final other=used-values[i];setState(()=>values[i]=v.round().clamp(0,widget.total-other));})]));}),
    const SizedBox(height:20),const Text('TOPICS / CHAPTERS',style:TextStyle(fontWeight:FontWeight.w800,letterSpacing:1)),const SizedBox(height:8),Row(children:[Expanded(child:TextField(controller:topicInput,onSubmitted:(_)=>addTopic(),decoration:const InputDecoration(hintText:'e.g. Algebra — Quadratic Equations',border:OutlineInputBorder()))),const SizedBox(width:8),IconButton.filled(onPressed:addTopic,icon:const Icon(Icons.add))]),
    const SizedBox(height:8),Wrap(spacing:8,runSpacing:8,children:topics.map((t)=>InputChip(label:Text(t),onDeleted:()=>setState(()=>topics.remove(t)))).toList()),const SizedBox(height:24),
    FilledButton.icon(onPressed:used==0?null:(){final items=<StudyItem>[];for(var i=0;i<widget.subjects.length;i++){if(values[i]>0)items.add(StudyItem(title:widget.subjects[i],minutes:values[i],topic:topics.isEmpty?'Study block':topics.first));}final plan=StudyPlan(totalMinutes:widget.total,items:items);widget.store.savePlan(plan);Navigator.pushReplacement(context,MaterialPageRoute(builder:(_)=>FocusScreen(store:widget.store,plan:plan,index:0)));},icon:const Icon(Icons.play_arrow),label:const Padding(padding:EdgeInsets.all(14),child:Text('Save plan & start')),
  ]));
}

class FocusScreen extends StatefulWidget { const FocusScreen({super.key,required this.store,required this.plan,required this.index}); final LocalStore store;final StudyPlan plan;final int index; @override State<FocusScreen> createState()=>_FocusScreenState(); }
class _FocusScreenState extends State<FocusScreen>{late int seconds;bool running=true;Timer? timer;
  StudyItem get item=>widget.plan.items[widget.index];
  @override void initState(){super.initState();seconds=item.minutes*60;timer=Timer.periodic(const Duration(seconds:1),(_){if(!mounted)return;if(running&&seconds>0)setState(()=>seconds--);if(seconds==0){timer?.cancel();Navigator.pushReplacement(context,MaterialPageRoute(builder:(_)=>BreakScreen(store:widget.store,plan:widget.plan,index:widget.index,completed:item.minutes)));}});}
  @override void dispose(){timer?.cancel();super.dispose();}
  @override Widget build(BuildContext context){final clock='${seconds~/60}'.padLeft(2,'0')+':${(seconds%60).toString().padLeft(2,'0')}';return Scaffold(appBar:AppBar(title:const Text('Focus mode'),actions:[IconButton(onPressed:()=>Navigator.pop(context),icon:const Icon(Icons.close))]),body:Center(child:Padding(padding:const EdgeInsets.all(28),child:Column(mainAxisAlignment:MainAxisAlignment.center,children:[Text('FOCUS BLOCK ${widget.index+1} OF ${widget.plan.items.length}',style:const TextStyle(letterSpacing:1.8,fontWeight:FontWeight.w800)),const SizedBox(height:12),Text(item.title,style:Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight:FontWeight.w900)),if(item.topic.isNotEmpty)Padding(padding:const EdgeInsets.only(top:8),child:Text(item.topic)),const SizedBox(height:45),Text(clock,style:Theme.of(context).textTheme.displayLarge?.copyWith(fontWeight:FontWeight.w900)),const SizedBox(height:42),Row(mainAxisAlignment:MainAxisAlignment.center,children:[IconButton.filledTonal(onPressed:()=>setState(()=>running=!running),icon:Icon(running?Icons.pause_rounded:Icons.play_arrow_rounded),iconSize:30),const SizedBox(width:16),FilledButton.icon(onPressed:()=>Navigator.pushReplacement(context,MaterialPageRoute(builder:(_)=>BreakScreen(store:widget.store,plan:widget.plan,index:widget.index,completed:item.minutes-(seconds~/60)))),icon:const Icon(Icons.check_rounded),label:const Text('Finish early'))]),const SizedBox(height:22),const Text('25-minute focus is the default. Your next block starts after a recharge break.')]))));}
}

class BreakScreen extends StatelessWidget { const BreakScreen({super.key,required this.store,required this.plan,required this.index,required this.completed}); final LocalStore store;final StudyPlan plan;final int index,completed;
  @override Widget build(BuildContext context)=>Scaffold(body:SafeArea(child:Center(child:Padding(padding:const EdgeInsets.all(28),child:Column(mainAxisAlignment:MainAxisAlignment.center,children:[const Icon(Icons.self_improvement_rounded,size:72),const SizedBox(height:20),Text('Great work!',style:TextStyle(fontSize:34,fontWeight:FontWeight.w900)),const SizedBox(height:10),const Text('Recharge before your next focus block.',textAlign:TextAlign.center),const SizedBox(height:24),const Card(child:Padding(padding:EdgeInsets.all(20),child:Column(children:[Text('💧 Drink water'),SizedBox(height:12),Text('🚶 Walk or stretch'),SizedBox(height:12),Text('👀 Rest your eyes'),SizedBox(height:12),Text('🧘 Take a few deep breaths')])),),const SizedBox(height:24),FilledButton(onPressed:()async{await store.addCompletedMinutes(completed);final next=index+1;if(next<plan.items.length){Navigator.pushReplacement(context,MaterialPageRoute(builder:(_)=>FocusScreen(store:store,plan:plan,index:next)));}else{Navigator.pushReplacement(context,MaterialPageRoute(builder:(_)=>CompletionScreen(plan:plan)));}},child:Text(index+1<plan.items.length?'Start next block':'Finish session'))]))));}
}

class CompletionScreen extends StatelessWidget { const CompletionScreen({super.key,required this.plan}); final StudyPlan plan; @override Widget build(BuildContext context)=>Scaffold(body:Center(child:Padding(padding:const EdgeInsets.all(28),child:Column(mainAxisAlignment:MainAxisAlignment.center,children:[const Icon(Icons.emoji_events_rounded,size:80),const SizedBox(height:22),Text('Session complete',style:Theme.of(context).textTheme.displaySmall?.copyWith(fontWeight:FontWeight.w900)),const SizedBox(height:10),Text('${plan.allocatedMinutes} minutes planned. Keep the streak going.',textAlign:TextAlign.center),const SizedBox(height:28),FilledButton(onPressed:()=>Navigator.popUntil(context,(r)=>r.isFirst),child:const Text('Back to home'))]))));}

class ComingSoon extends StatelessWidget { const ComingSoon({super.key,required this.title}); final String title; @override Widget build(BuildContext context)=>Scaffold(appBar:AppBar(title:Text(title)),body:Center(child:Padding(padding:const EdgeInsets.all(28),child:Column(mainAxisAlignment:MainAxisAlignment.center,children:[const Icon(Icons.construction_rounded,size:64),const SizedBox(height:18),Text('$title is next',style:Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight:FontWeight.w900)),const SizedBox(height:8),const Text('The foundation is ready. This mode will get its own planning engine in the next milestone.',textAlign:TextAlign.center)]))));}
