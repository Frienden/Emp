package dev.resourcevision;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class ResourceVisionClient implements ClientModInitializer {
 public static ResourceVisionClient INSTANCE;
 public VisionConfig config;
 public final Map<BlockPos,Integer> found = new HashMap<>();
 private final IdentityHashMap<Block,Integer> watched = new IdentityHashMap<>();
 private final List<BlockPos> nearest = new ArrayList<>();
 private KeyBinding pick,toggle,menu,clear;
 private ClientWorld lastWorld;
 private int cursor=0,tickCount=0;
 private BlockPos anchor;

 @Override public void onInitializeClient() {
   INSTANCE=this; config=VisionConfig.load(); updateWatched();
   pick=bind("pick",GLFW.GLFW_KEY_F6); toggle=bind("toggle",GLFW.GLFW_KEY_F7);
   menu=bind("menu",GLFW.GLFW_KEY_F8); clear=bind("clear",GLFW.GLFW_KEY_F9);
   ClientTickEvents.END_CLIENT_TICK.register(this::tick);
   HudRenderCallback.EVENT.register((context,delta)->drawHud(context));
   WorldRenderEvents.LAST.register(context->VisionRenderer.render(context,this));
 }
 private static KeyBinding bind(String name,int key) {
   return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.resourcevision."+name, InputUtil.Type.KEYSYM,key,"category.resourcevision"));
 }
 private void tick(MinecraftClient client) {
   if(lastWorld!=client.world){lastWorld=client.world;clearFindings();}
   while(pick.wasPressed()) pickLookingAt(client);
   while(toggle.wasPressed()){config.enabled=!config.enabled;config.save();}
   while(menu.wasPressed()) client.setScreen(new VisionScreen(this));
   while(clear.wasPressed()) clearFindings();
   if(client.player==null||client.world==null)return;
   if(config.enabled&&!watched.isEmpty()&&client.currentScreen==null) scan(client.world,client.player.getBlockPos());
   if(++tickCount%20==0){prune(client.world,client.player.getBlockPos());sortNearest(client.player.getBlockPos());}
 }
 private void pickLookingAt(MinecraftClient client){
   if(client.world==null||!(client.crosshairTarget instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK)return;
   Block block=client.world.getBlockState(hit.getBlockPos()).getBlock();
   String id=Registries.BLOCK.getId(block).toString();
   if(config.targets.containsKey(id)) config.targets.remove(id);
   else if(config.targets.size()<12) config.targets.put(id,config.autoColor());
   changedTargets();
 }
 public void changedTargets(){updateWatched();clearFindings();config.save();}
 public void changedSettings(boolean clear){if(clear)clearFindings();config.save();}
 public void clearFindings(){found.clear();nearest.clear();anchor=null;cursor=0;}
 private void updateWatched(){
   watched.clear();
   config.targets.forEach((name,color)->{
     try{var id=new net.minecraft.util.Identifier(name);if(Registries.BLOCK.containsId(id))watched.put(Registries.BLOCK.get(id),color);}
     catch(RuntimeException ignored){}
   });
 }
 private static int offset(int n){return n==0?0:n%2==1?(n+1)/2:-n/2;}
 private void scan(ClientWorld world,BlockPos player){
   if(anchor==null||anchor.getSquaredDistance(player)>36){anchor=player;cursor=0;}
   int r=config.radius, v=config.verticalRadius;
   BlockPos.Mutable pos=new BlockPos.Mutable();
   for(int slice=0;slice<2;slice++){
     int dx=offset(cursor++);if(cursor>2*r)cursor=0;
     for(int dz=-r;dz<=r;dz++){
       if(dx*dx+dz*dz>r*r)continue;
       int x=player.getX()+dx,z=player.getZ()+dz;
       if(!world.isChunkLoaded(x>>4,z>>4))continue;
       for(int dy=-v;dy<=v;dy++){
         if(found.size()>=config.maxMarkers)return;
         int y=player.getY()+dy;
         if(y<world.getBottomY()||y>=world.getTopY())continue;
         pos.set(x,y,z);
         Integer c=watched.get(world.getBlockState(pos).getBlock());
         if(c!=null&&(!config.exposedOnly||exposed(world,pos)))found.put(pos.toImmutable(),c);
       }
     }
   }
 }
 private static boolean exposed(ClientWorld world,BlockPos pos){
   for(Direction dir:Direction.values())if(world.getBlockState(pos.offset(dir)).isAir())return true;
   return false;
 }
 private void prune(ClientWorld world,BlockPos player){
   int r=config.radius,v=config.verticalRadius;
   found.entrySet().removeIf(entry->{
     BlockPos p=entry.getKey();int dx=p.getX()-player.getX(),dz=p.getZ()-player.getZ();
     return dx*dx+dz*dz>r*r||Math.abs(p.getY()-player.getY())>v||
       !world.isChunkLoaded(p.getX()>>4,p.getZ()>>4)||
       !watched.containsKey(world.getBlockState(p).getBlock())||
       config.exposedOnly&&!exposed(world,p);
   });
 }
 private void sortNearest(BlockPos player){
   nearest.clear();nearest.addAll(found.keySet());nearest.sort(Comparator.comparingDouble(p->p.getSquaredDistance(player)));
   if(nearest.size()>4)nearest.subList(4,nearest.size()).clear();
 }
 public List<BlockPos> nearest(){return nearest;}
 private void drawHud(net.minecraft.client.gui.DrawContext ctx){
   var client=MinecraftClient.getInstance();
   if(client.world==null||client.player==null||client.options.hudHidden||!config.showHud)return;
   int x=Math.max(8,client.getWindow().getScaledWidth()-275),y=8;
   ctx.fill(x-5,y-5,x+260,y+34+12*Math.max(1,nearest.size()),0xBA101522);
   ctx.drawTextWithShadow(client.textRenderer,Text.literal("RESOURCE VISION ["+(config.enabled?"ON":"OFF")+"]  F8: MENU"),x,y,0x66E5FF);
   ctx.drawTextWithShadow(client.textRenderer,Text.literal("Targets: "+config.targets.size()+" | Found: "+found.size()+" | Radius: "+config.radius),x,y+12,0xFFFFFF);
   if(!config.enabled)return;
   int i=0;for(BlockPos p:nearest){
     int distance=(int)Math.sqrt(p.getSquaredDistance(client.player.getBlockPos()));
     ctx.drawTextWithShadow(client.textRenderer,Text.literal(p.getX()+", "+p.getY()+", "+p.getZ()+"  "+distance+"m"),x,y+27+12*i++,found.getOrDefault(p,0xFFFFFF));
   }
 }
}