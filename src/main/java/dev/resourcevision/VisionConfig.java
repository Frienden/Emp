package dev.resourcevision;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;
public final class VisionConfig {
 private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
 private static final int[] COLORS={0x39D9FF,0xFFC857,0xAE86FF,0xFF6486,0x66F29B,0xFF9B42,0xF0F078,0x5BC0FF,0xFF85E8,0x9DFF85,0xF7F7F7,0xC0AA81};
 public boolean enabled=true,throughWalls=true,exposedOnly=false,showHud=true,tracers=false;
 public int radius=32,verticalRadius=24,maxMarkers=750;
 public LinkedHashMap<String,Integer> targets=new LinkedHashMap<>();
 private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("resource-vision.json");}
 public static VisionConfig load(){
   try {
     if(Files.exists(path())) {
       VisionConfig c=GSON.fromJson(Files.readString(path()),VisionConfig.class);
       if(c!=null) {
         c.radius=Math.max(8,Math.min(64,c.radius));
         c.verticalRadius=Math.max(8,Math.min(48,c.verticalRadius));
         c.maxMarkers=Math.max(100,Math.min(1500,c.maxMarkers));
         if(c.targets==null)c.targets=new LinkedHashMap<>();
         if(c.targets.size()>12)c.targets=new LinkedHashMap<>(c.targets.entrySet().stream().limit(12).collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue,(a,b)->a,LinkedHashMap::new)));
         return c;
       }
     }
   }catch(Exception ex){System.err.println("[Resource Vision] "+ex);}
   VisionConfig c=new VisionConfig();
   c.targets.put("minecraft:diamond_ore",COLORS[0]);
   c.targets.put("minecraft:deepslate_diamond_ore",COLORS[0]);
   c.targets.put("minecraft:diorite",COLORS[1]);
   return c;
 }
 public void save(){try{Files.createDirectories(path().getParent());Files.writeString(path(),GSON.toJson(this));}catch(Exception ex){System.err.println("[Resource Vision] "+ex);}}
 public int autoColor(){return COLORS[targets.size()%COLORS.length];}
 public int nextColor(int c){for(int i=0;i<COLORS.length;i++)if(COLORS[i]==c)return COLORS[(i+1)%COLORS.length];return COLORS[0];}
}