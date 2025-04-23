package com.redstoned.sharedinv;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.entity.EntityEquipment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.*;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class EntityEquipmentHollower implements Opcodes {
	private static final MappingResolver mr = FabricLoader.getInstance().getMappingResolver();
	private static final String iName = mr.unmapClassName("intermediary", EntityEquipment.class.getName());

	private static String mapMethod(String methodName, String descriptor) {
		return mr.mapMethodName("intermediary", iName, methodName, descriptor);
	}

	private static String mapField(String fieldName, String descriptor) {
		return mr.mapFieldName("intermediary", iName, fieldName, descriptor);
	}

	private static String desc(Class<?> result, Class<?>... in) {
		StringBuilder sb = new StringBuilder("(");
		for (Class<?> c : in) sb.append(lname(c));
		sb.append(")").append(lname(result));
		return sb.toString();
	}

	private static Wrap hollowed;

	public static EntityEquipment wrap(EntityEquipment delegate, PlayerEntity player) {
		if (hollowed == null) {
			synchronized (EntityEquipmentHollower.class) {
				if (hollowed == null) {
					try {
						hollowed = hollow();
					} catch (Throwable e) {
						throw new RuntimeException("Could not hollow EntityEquipment", e);
					}
				}
			}
		}
		return hollowed.wrap(delegate, player);
	}

	private interface Wrap {
		EntityEquipment wrap(EntityEquipment delegate, PlayerEntity player);
	}

	private static Wrap hollow() throws Throwable {
		List<Method> methods = Arrays.stream(EntityEquipment.class.getDeclaredMethods())
				.filter(s -> handle(s.accessFlags()))
				.toList();

		String cn = "com/redstoned/sharedinv/HollowedEntityEquipment";
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V21, ACC_PUBLIC | ACC_SUPER, cn, null, name(EntityEquipment.class), null);

		cw.visitField(ACC_PRIVATE | ACC_FINAL, "delegate", lname(EntityEquipment.class), null, null).visitEnd();
		cw.visitField(ACC_PRIVATE | ACC_FINAL, "player", lname(PlayerEntity.class), null, null).visitEnd();

		MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "<init>", desc(void.class, EntityEquipment.class, PlayerEntity.class), null, null);
		mw.visitCode();
		mw.visitVarInsn(ALOAD, 0);
		mw.visitVarInsn(ALOAD, 1);
		String fieldDesc = lname(EnumMap.class);
		mw.visitFieldInsn(GETFIELD, name(EntityEquipment.class), mapField("field_55944", fieldDesc), fieldDesc);
		mw.visitMethodInsn(INVOKESPECIAL, name(EntityEquipment.class), "<init>", desc(void.class, EnumMap.class), false);
		mw.visitVarInsn(ALOAD, 0);
		mw.visitInsn(DUP);
		mw.visitVarInsn(ALOAD, 1);
		mw.visitFieldInsn(PUTFIELD, cn, "delegate", lname(EntityEquipment.class));
		mw.visitVarInsn(ALOAD, 2);
		mw.visitFieldInsn(PUTFIELD, cn, "player", lname(PlayerEntity.class));
		mw.visitInsn(RETURN);
		mw.visitMaxs(3, 3);
		mw.visitEnd();

		for (Method method : methods) {
			String desc = desc(method.getReturnType(), method.getParameterTypes());
			mw = cw.visitMethod(ACC_PUBLIC, method.getName(), desc, null, null);
			mw.visitCode();
			if (!handled.containsKey(method.getName())) {
				mw.visitVarInsn(ALOAD, 0);
				mw.visitFieldInsn(GETFIELD, cn, "delegate", lname(EntityEquipment.class));
			}
			for (int i = 0; i < method.getParameterCount(); i++) {
				mw.visitVarInsn(ALOAD, i + 1);
			}
			if (handled.containsKey(method.getName())) {
				mw.visitVarInsn(ALOAD, 0);
				mw.visitFieldInsn(GETFIELD, cn, "delegate", lname(EntityEquipment.class));
				mw.visitVarInsn(ALOAD, 0);
				mw.visitFieldInsn(GETFIELD, cn, "player", lname(PlayerEntity.class));
				String hdesc = desc.replace(")", lname(EntityEquipment.class) + lname(PlayerEntity.class) + ")");
				mw.visitMethodInsn(INVOKESTATIC, name(EntityEquipmentHollower.class), method.getName(), hdesc, false);
			} else {
				mw.visitMethodInsn(INVOKEVIRTUAL, name(EntityEquipment.class), method.getName(), desc, false);
			}
			if (method.getReturnType().equals(boolean.class)) mw.visitInsn(IRETURN);
			else if (method.getReturnType().equals(void.class)) mw.visitInsn(RETURN);
			else mw.visitInsn(ARETURN);
			mw.visitMaxs(method.getParameterCount() + 2, method.getParameterCount() + 1);
			mw.visitEnd();
		}

		cw.visitEnd();
		MethodHandles.Lookup lookup = MethodHandles.lookup();

		Class<?> clazz = lookup.defineClass(cw.toByteArray());

		Files.write(Path.of("x.class"), cw.toByteArray());

		return (Wrap) LambdaMetafactory.metafactory(
				lookup,
				"wrap",
				MethodType.methodType(Wrap.class),
				MethodType.methodType(EntityEquipment.class, EntityEquipment.class, PlayerEntity.class),
				lookup.unreflectConstructor(clazz.getConstructors()[0]),
				MethodType.methodType(clazz, EntityEquipment.class, PlayerEntity.class)
		).getTarget().invoke();
	}

	private static boolean handle(Set<AccessFlag> flags) {
		if (flags.contains(AccessFlag.STATIC)) return false;
		return flags.contains(AccessFlag.PUBLIC) || flags.contains(AccessFlag.PROTECTED);
	}

	private static String name(Class<?> clazz) {
		return clazz.getName().replace('.', '/');
	}

	private static String lname(Class<?> clazz) {
		// Does not handle everything, but should be enough for our purposes
		if (clazz.equals(void.class)) return "V";
		if (clazz.equals(boolean.class)) return "Z";
		return "L" + name(clazz) + ";";
	}


	// This mirrors the methods in PlayerEquipment but with the player injected later
	private static final Map<String, String> handled = Map.of(
			"put", mapMethod("method_66660", desc(ItemStack.class, EquipmentSlot.class, ItemStack.class)),
			"get", mapMethod("method_66659", desc(ItemStack.class, EquipmentSlot.class)),
			"isEmpty", mapMethod("method_66657", desc(boolean.class))
	);

	public static ItemStack put(EquipmentSlot slot, ItemStack stack, EntityEquipment delegate, PlayerEntity player) {
		return slot == EquipmentSlot.MAINHAND ? player.getInventory().setSelectedStack(stack) : delegate.put(slot, stack);
	}

	public static ItemStack get(EquipmentSlot slot, EntityEquipment delegate, PlayerEntity player) {
		return slot == EquipmentSlot.MAINHAND ? player.getInventory().getSelectedStack() : delegate.get(slot);
	}

	public static boolean isEmpty(EntityEquipment delegate, PlayerEntity player) {
		return player.getInventory().getSelectedStack().isEmpty() && delegate.isEmpty();
	}
}
