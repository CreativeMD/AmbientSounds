package com.creativemd.ambientsounds.soundfix;

import java.util.Iterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.creativemd.creativecore.transformer.CreativeTransformer;
import com.creativemd.creativecore.transformer.Transformer;

import paulscode.sound.Library;

public class SoundFixTransformer extends CreativeTransformer {

	public SoundFixTransformer() {
		super("soundfix");
	}

	@Override
	protected void initTransformers() {
		addTransformer(new Transformer("paulscode.sound.Source") {
			
			@Override
			public void transform(ClassNode node) {
				node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "removed", "Z", null, null));
			}
		});
		
		addTransformer(new Transformer("paulscode.sound.Library") {
			
			@Override
			public void transform(ClassNode node) {
				MethodNode m = findMethod(node, "removeSource", "(Ljava/lang/String;)V");
				for (Iterator iterator = m.instructions.iterator(); iterator.hasNext();) {
					AbstractInsnNode insn = (AbstractInsnNode) iterator.next();	
					if(insn instanceof MethodInsnNode && ((MethodInsnNode) insn).owner.equals("paulscode/sound/Source") && ((MethodInsnNode) insn).name.equals("cleanup"))
					{
						m.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/creativemd/ambientsounds/soundfix/SoundFixMethods", "cleanupSource", "(Lpaulscode/sound/Source;)V", false));
						m.instructions.remove(insn);
						break;
					}
				}
			}
		});
		
		addTransformer(new Transformer("paulscode.sound.StreamThread") {
			
			@Override
			public void transform(ClassNode node) {
				MethodNode m = findMethod(node, "run", "()V");
				for (Iterator iterator = m.instructions.iterator(); iterator.hasNext();) {
					AbstractInsnNode insn = (AbstractInsnNode) iterator.next();
					if(insn instanceof MethodInsnNode && ((MethodInsnNode) insn).owner.equals("java/util/ListIterator") && ((MethodInsnNode) insn).name.equals("next"))
					{
						insn = insn.getNext().getNext();
						
						int varIndex = 2;
						
						m.instructions.insert(insn, new VarInsnNode(Opcodes.ASTORE, varIndex));
						m.instructions.insert(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/creativemd/ambientsounds/soundfix/SoundFixMethods", "removeSource", "(Lpaulscode/sound/Source;)Lpaulscode/sound/Source;", false));
						m.instructions.insert(insn, new VarInsnNode(Opcodes.ALOAD, varIndex));
						break;
					}
				}
				
			}
		});
	}

}
