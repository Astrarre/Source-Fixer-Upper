import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class RepackagedJavacProvider {

    public static final Type STRING = Type.getType(String.class);

    public static Path createJavacJar(File buildDir) throws IOException {
        Files.createDirectories(buildDir.toPath());
        Path of = buildDir.toPath().resolve("javac_sfu_rpkg.jar");

        if (Files.exists(of)) {
            return of;
        }

        Path compiler = FileSystems.getFileSystem(URI.create("jrt:/")).getPath("modules", "jdk.compiler");
        Set<String> classes = Files.walk(compiler)
                .filter(p -> p.toString().endsWith(".class"))
                .map(p -> {
                    String classFile = compiler.relativize(p).toString();
                    return classFile.substring(0, classFile.length() - 6);
                })
                .collect(Collectors.toUnmodifiableSet());

        class Repackager extends Remapper {
            @Override
            public String map(String internalName) {
                return classes.contains(internalName) ? repackage(internalName) : internalName;
            }
        }

        class ResourceBundleRemapper extends MethodNode {
            final String owner;
            final MethodVisitor visitor;
            List<RepackageNode> nodes;

            record RepackageNode(int index, int stack) {
            }

            public ResourceBundleRemapper(int api, int access, String name, String descriptor, String signature, String[] exceptions, String owner, MethodVisitor visitor) {
                super(api, access, name, descriptor, signature, exceptions);
                this.owner = owner;
                this.visitor = visitor;
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if ((owner.equals("java/util/ResourceBundle") && name.equals("getBundle") && descriptor.startsWith("(Ljava/lang/String;")) ||
                        (owner.equals("java/lang/Class") && name.equals("forName"))) {
                    List<RepackageNode> nodes = this.nodes;
                    if (nodes == null) {
                        this.nodes = nodes = new ArrayList<>();
                    }

                    Type[] args = Type.getArgumentTypes(descriptor);
                    nodes.add(new RepackageNode(this.instructions.size(), (args.length - 1) - Arrays.asList(args).indexOf(STRING))); // breaks with longs
                }

                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

                if (owner.equals("java/lang/Class") && name.equals("getName") && descriptor.equals("()Ljava/lang/String;")) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/astrarre/sfu/impl/JavacHack", "unrepackage", "(Ljava/lang/String;)Ljava/lang/String;", false);
                }
            }

            @Override
            public void visitInsn(int opcode) {
                if (opcode == Opcodes.ARETURN && this.owner.equals("com/sun/tools/javac/resources/ct")) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/astrarre/sfu/impl/JavacHack", "addConstant", "([[Ljava/lang/Object;)[[Ljava/lang/Object;", false);
                }

                super.visitInsn(opcode);
            }

            @Override
            public void visitEnd() {
                if (this.nodes != null) {
                    Interpreter<SourceValue> interpreter = new SourceInterpreter();
                    Analyzer<SourceValue> analyzer = new Analyzer<>(interpreter);
                    try {
                        this.tryCatchBlocks = List.of();
                        Frame<SourceValue>[] analyze = analyzer.analyze(this.owner, this);
                        for (RepackageNode node : nodes) {
                            Frame<SourceValue> frame = analyze[node.index];
                            AbstractInsnNode string = frame.getStack((frame.getStackSize() - 1) - node.stack).insns.iterator().next();
                            MethodInsnNode repackage = new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "io/github/astrarre/sfu/impl/JavacHack",
                                    "repackage",
                                    "(Ljava/lang/String;)Ljava/lang/String;");
                            this.instructions.insert(string, repackage);
                        }
                    } catch (AnalyzerException e) {
                        throw new RuntimeException(this.owner, e);
                    }
                }

                this.accept(this.visitor);
            }
        }

        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(of))) {
            Files.walkFileTree(compiler, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!Files.isRegularFile(file)) {
                        return FileVisitResult.CONTINUE;
                    }

                    String name = compiler.relativize(file).toString();

                    if ("module-info.class".equals(name)) {
                        // skip
                    } else if (name.endsWith(".class")) {
                        ClassWriter writer = new ClassWriter(0);
                        new ClassReader(Files.readAllBytes(file)).accept(new ClassRemapper(writer, new Repackager()) {
                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                return new ResourceBundleRemapper(
                                        Opcodes.ASM9,
                                        access,
                                        name,
                                        descriptor,
                                        signature,
                                        exceptions,
                                        this.className,
                                        super.visitMethod(access, name, descriptor, signature, exceptions));
                            }
                        }, 0);
                        jar.putNextEntry(new JarEntry(repackage(name)));
                        jar.write(writer.toByteArray());
                    } else {
                        jar.putNextEntry(new JarEntry(name));
                        Files.copy(file, jar);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return of;
    }

    private static String repackage(String path) {
        return "sfu_rpkg/" + path;
    }
}
