package dev.resourcevision;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import java.util.*;

public final class VisionScreen extends Screen {
 private static final int WIDTH=316,ROWS=6;
 private final ResourceVisionClient vision;
 private final List<Block> all=new ArrayList<>(),matches=new ArrayList<>();
 private final ButtonWidget[] rows=new ButtonWidget[ROWS];
 private TextFieldWidget search;
 private ButtonWidget pageLabel,radius,height,hud,walls,exposed,tracers;
 private int left,top,page=0;
 private String query="";
 public VisionScreen(ResourceVisionClient vision){
   super(Text.literal("Resource Vision"));
   this.vision=vision;
   Registries.BLOCK.stream().forEach(all::add);
   all.sort(Comparator.comparing(b->b.getName().getString().toLowerCase(Locale.ROOT)));
 }
 @Override protected void init(){
   left=width/2-WIDTH/2;top=Math.max(4,height/2-153);
   search=new TextFieldWidget(textRenderer,left+8,top+30,WIDTH-16,19,Text.literal("Block search"));
   search.setMaxLength(120);
   search.setChangedListener(q->{query=q;page=0;filter();});
   addDrawableChild(search);search.setText(query);setInitialFocus(search);
   for(int i=0;i<ROWS;i++){
     final int k=i;
     rows[i]=addDrawableChild(ButtonWidget.builder(Text.empty(),b->select(k)).dimensions(left+8,top+55+i*22,WIDTH-16,20).build());
   }
   addDrawableChild(ButtonWidget.builder(Text.literal("<"),b->{page=Math.max(0,page-1);refresh();}).dimensions(left+8,top+189,43,20).build());
   pageLabel=addDrawableChild(ButtonWidget.builder(Text.empty(),b->{}).dimensions(left+56,top+189,WIDTH-112,20).build());
   pageLabel.active=false;
   addDrawableChild(ButtonWidget.builder(Text.literal(">"),b->{page=Math.min(lastPage(),page+1);refresh();}).dimensions(left+WIDTH-51,top+189,43,20).build());
   radius=addDrawableChild(ButtonWidget.builder(Text.empty(),b->{vision.config.radius=cycle(vision.config.radius,new int[]{8,16,24,32,48,64});vision.changedSettings(true);refresh();}).dimensions(left+8,top+223,96,20).build());
   height=addDrawableChild(ButtonWidget.builder(Text.empty(),b->{vision.config.verticalRadius=cycle(vision.config.verticalRadius,new int[]{8,16,24,32,48});vision.changedSettings(true);refresh();}).dimensions(left+110,top+223,96,20).build());
   hud=addDrawableChild(ButtonWidget.builder(Text.empty(),b->{vision.config.showHud=!vision.config.showHud;vision.changedSettings(false);refresh();}).dimensions(left+212,top+223,96,20).build());
   walls=addDrawableChild(ButtonWidget.builder(Text.empty(),b->{vision.config.throughWalls=!vision.config.throughWalls;vision.changedSettings(false);refresh();}).dimensions(left+8,top+247,96,20).build());
   exposed=addDrawableChild(ButtonWidget.builder(Text.empty(),b->{vision.config.exposedOnly=!vision.config.exposedOnly;vision.changedSettings(true);refresh();}).dimensions(left+110,top+247,96,20).build());
   tracers=addDrawableChild(ButtonWidget.builder(Text.empty(),b->{vision.config.tracers=!vision.config.tracers;vision.changedSettings(false);refresh();}).dimensions(left+212,top+247,96,20).build());
   addDrawableChild(ButtonWidget.builder(Text.literal("Clear"),b->vision.clearFindings()).dimensions(left+8,top+273,92,20).build());
   addDrawableChild(ButtonWidget.builder(Text.literal("Reset targets"),b->{vision.config.targets.clear();vision.changedTargets();refresh();}).dimensions(left+106,top+273,98,20).build());
   addDrawableChild(ButtonWidget.builder(Text.literal("Done"),b->close()).dimensions(left+210,top+273,98,20).build());
   filter();
 }
 private void filter(){
   matches.clear();
   String q=query.strip().toLowerCase(Locale.ROOT);
   for(Block b:all)if(q.isEmpty()||b.getName().getString().toLowerCase(Locale.ROOT).contains(q)||Registries.BLOCK.getId(b).toString().contains(q))matches.add(b);
   page=Math.min(page,lastPage());refresh();
 }
 private void select(int row){
   int i=page*ROWS+row;if(i>=matches.size())return;
   String id=Registries.BLOCK.getId(matches.get(i)).toString();
   Integer c=vision.config.targets.get(id);
   if(c!=null&&hasShiftDown())vision.config.targets.put(id,vision.config.nextColor(c));
   else if(c!=null)vision.config.targets.remove(id);
   else if(vision.config.targets.size()<12)vision.config.targets.put(id,vision.config.autoColor());
   vision.changedTargets();refresh();
 }
 private int lastPage(){return Math.max(0,(matches.size()-1)/ROWS);}
 private void refresh(){
   if(rows[0]==null)return;
   for(int i=0;i<ROWS;i++){
     int index=page*ROWS+i;boolean exists=index<matches.size();rows[i].visible=exists;
     if(!exists)continue;
     Block b=matches.get(index);String id=Registries.BLOCK.getId(b).toString();
     Integer color=vision.config.targets.get(id);
     String label=(color==null?"[ ] ":"[X] ")+trim(b.getName().getString(),25)+" ("+trim(id,28)+")";
     rows[i].setMessage(Text.literal(label).styled(s->s.withColor(color==null?0xD2D8DF:color)));
   }
   if(pageLabel==null)return;
   pageLabel.setMessage(Text.literal("Page "+(page+1)+"/"+(lastPage()+1)+" ("+matches.size()+")"));
   radius.setMessage(Text.literal("Radius "+vision.config.radius));
   height.setMessage(Text.literal("Height "+vision.config.verticalRadius));
   hud.setMessage(Text.literal("HUD "+yes(vision.config.showHud)));
   walls.setMessage(Text.literal("X-Ray "+yes(vision.config.throughWalls)));
   exposed.setMessage(Text.literal("Exposed "+yes(vision.config.exposedOnly)));
   tracers.setMessage(Text.literal("Tracers "+yes(vision.config.tracers)));
 }
 private static String yes(boolean v){return v?"ON":"OFF";}
 private static String trim(String s,int n){return s.length()>n?s.substring(0,n-1)+"…":s;}
 private static int cycle(int old,int[] values){for(int i=0;i<values.length;i++)if(values[i]==old)return values[(i+1)%values.length];return values[0];}
 @Override public void render(DrawContext draw,int mouseX,int mouseY,float delta){
   renderBackground(draw);
   draw.fill(left,top,left+WIDTH,top+299,0xDB0A1220);
   draw.fill(left,top,left+WIDTH,top+2,0xFF30D2E6);
   draw.drawCenteredTextWithShadow(textRenderer,Text.literal("RESOURCE VISION | "+vision.config.targets.size()+"/12 TARGETS"),width/2,top+9,0xFFFFFF);
   draw.drawTextWithShadow(textRenderer,"Search by block name or minecraft:id",left+8,top+20,0xA5BBCD);
   draw.drawTextWithShadow(textRenderer,"Click: toggle | Shift+click: next color",left+8,top+212,0xA5BBCD);
   super.render(draw,mouseX,mouseY,delta);
 }
 @Override public boolean shouldPause(){return false;}
}