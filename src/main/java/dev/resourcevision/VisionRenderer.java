package dev.resourcevision;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.Map;

public final class VisionRenderer {
 private VisionRenderer(){}
 public static void render(WorldRenderContext context, ResourceVisionClient vision) {
   if(!vision.config.enabled||vision.found.isEmpty()||context.matrixStack()==null)return;
   var client=MinecraftClient.getInstance();
   if(client.world==null||client.player==null)return;
   MatrixStack matrices=context.matrixStack();
   Vec3d camera=context.camera().getPos();
   matrices.push();
   matrices.translate(-camera.x,-camera.y,-camera.z);
   RenderSystem.enableBlend();
   RenderSystem.defaultBlendFunc();
   RenderSystem.disableCull();
   if(vision.config.throughWalls)RenderSystem.disableDepthTest();
   RenderSystem.depthMask(false);
   RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
   RenderSystem.lineWidth(2f);
   Tessellator tess=Tessellator.getInstance();
   BufferBuilder buffer=tess.getBuffer();
   buffer.begin(VertexFormat.DrawMode.LINES,VertexFormats.LINES);
   for(Map.Entry<BlockPos,Integer> entry:vision.found.entrySet()){
     BlockPos pos=entry.getKey();
     int rgb=entry.getValue();
     float red=((rgb>>16)&255)/255f,green=((rgb>>8)&255)/255f,blue=(rgb&255)/255f;
     WorldRenderer.drawBox(matrices,buffer,
       pos.getX()+0.01,pos.getY()+0.01,pos.getZ()+0.01,
       pos.getX()+0.99,pos.getY()+0.99,pos.getZ()+0.99,
       red,green,blue,1f);
   }
   if(vision.config.tracers){
     for(BlockPos pos:vision.nearest()){
       Integer rgb=vision.found.get(pos);if(rgb==null)continue;
       Vec3d to=Vec3d.ofCenter(pos),from=camera.add(0,-0.2,0),normal=to.subtract(from).normalize();
       float r=((rgb>>16)&255)/255f,g=((rgb>>8)&255)/255f,b=(rgb&255)/255f;
       MatrixStack.Entry mat=matrices.peek();
       buffer.vertex(mat.getPositionMatrix(),(float)from.x,(float)from.y,(float)from.z).color(r,g,b,1f).normal(mat.getNormalMatrix(),(float)normal.x,(float)normal.y,(float)normal.z).next();
       buffer.vertex(mat.getPositionMatrix(),(float)to.x,(float)to.y,(float)to.z).color(r,g,b,1f).normal(mat.getNormalMatrix(),(float)normal.x,(float)normal.y,(float)normal.z).next();
     }
   }
   tess.draw();
   matrices.pop();
   RenderSystem.depthMask(true);
   RenderSystem.enableDepthTest();
   RenderSystem.enableCull();
   RenderSystem.disableBlend();
 }
}