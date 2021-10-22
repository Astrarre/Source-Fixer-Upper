import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;


public class RepackagedJavacProvider {
    public static final Type STRING = Type.getType(String.class);
    public static Path createJavacJar(File buildDir) throws IOException {
        Path of = Path.of(buildDir.getPath(), "javac_sfu_rpkg.jar");
        try(FileSystem system = createZip(of)) {
            Path root = system.getPath("/");
            var path = FileSystems.getFileSystem(URI.create("jrt:/")).getPath("modules", "jdk.compiler");
            var classes = Files.walk(path).filter(p -> {
                String s = p.toString();
                return s.endsWith(".class");
            }).map(p -> {
                var classFile = path.relativize(p).toString();
                return classFile.substring(0, classFile.length() - 6);
            }).collect(Collectors.toUnmodifiableSet());
            class Repackager extends Remapper {
                @Override
                public String map(String internalName) {
                    return (classes.contains(internalName)) ? repackage(internalName) : internalName;
                }
            }

            class ResourceBundleRemapper extends MethodNode {
                final String owner;
                final MethodVisitor visitor;
                List<RepackageNode> nodes;

                record RepackageNode(int index, int stack) {}

                public ResourceBundleRemapper(String owner, MethodVisitor visitor) {
                    super(Opcodes.ASM9);
                    this.owner = owner;
                    this.visitor = visitor;
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if((name.equals("getBundle") && owner.equals("java/util/ResourceBundle") && descriptor.startsWith("(Ljava/lang/String;")) ||
                       (name.equals("forName") && owner.equals("java/lang/Class"))) {
                        var nodes = this.nodes;
                        if(nodes == null) {
                            this.nodes = nodes = new ArrayList<>();
                        }

                        Type[] args = Type.getArgumentTypes(descriptor);
                        nodes.add(new RepackageNode(this.instructions.size(), (args.length - 1) - Arrays.asList(args).indexOf(STRING))); // breaks with longs
                    }

                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

                    if(name.equals("getName") && owner.equals("java/lang/Class") && descriptor.equals("()Ljava/lang/String;")) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/astrarre/sfu/JavacHack", "unrepackage", "(Ljava/lang/String;)Ljava/lang/String;");
                    }
                }

                @Override
                public void visitInsn(int opcode) {
                    if(opcode == Opcodes.ARETURN && this.owner.equals("com/sun/tools/javac/resources/ct")) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/astrarre/sfu/JavacHack", "addConstant", "([[Ljava/lang/Object;)[[Ljava/lang/Object;");
                    }
                    super.visitInsn(opcode);
                }

                @Override
                public void visitEnd() {
                    if(this.nodes != null) {
                        Interpreter<SourceValue> interpreter = new SourceInterpreter();
                        Analyzer<SourceValue> analyzer = new Analyzer<>(interpreter);
                        try {
                            this.tryCatchBlocks = List.of();
                            Frame<SourceValue>[] analyze = analyzer.analyze(this.owner, this);
                            for(RepackageNode node : nodes) {
                                Frame<SourceValue> frame = analyze[node.index];
                                AbstractInsnNode string = frame.getStack((frame.getStackSize() - 1) - node.stack).insns.iterator().next();
                                MethodInsnNode repackage = new MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        "io/github/astrarre/sfu/JavacHack",
                                        "repackage",
                                        "(Ljava/lang/String;)Ljava/lang/String;");
                                this.instructions.insert(string, repackage);
                            }
                        } catch(AnalyzerException e) {
                            throw new RuntimeException(this.owner, e);
                        }
                    }

                    this.accept(this.visitor);
                }
            }
            Files.walk(path).filter(Files::isRegularFile).forEach(p -> {
                Path dstFile;
                Callable<?> after;
                if(p.toString().endsWith(".class")) {
                    dstFile = root.resolve(repackage(path.relativize(p).toString()));
                    after = () -> {
                        var reader = new ClassReader(Files.readAllBytes(p));
                        var writer = new ClassWriter(0);
                        var rpkgr = new ClassRemapper(writer, new Repackager()) {
                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                ResourceBundleRemapper remapper = new ResourceBundleRemapper(
                                        this.className,
                                        super.visitMethod(access, name, descriptor, signature, exceptions));
                                remapper.access = access;
                                remapper.name = name;
                                remapper.desc = descriptor;
                                remapper.signature = signature;
                                remapper.exceptions = exceptions == null ? null : new ArrayList<>(List.of(exceptions));
                                return remapper;
                            }
                        };
                        reader.accept(rpkgr, 0);
                        return Files.write(dstFile, writer.toByteArray());
                    };
                } else {
                    dstFile = root.resolve(path.relativize(p).toString());
                    after = () -> Files.copy(p, dstFile);
                }
                try {
                    Path parent = dstFile.getParent();
                    if(parent != null) {
                        Files.createDirectories(parent);
                    }
                    after.call();
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return of;
    }

    static String repackage(String path) {
        int lastIndex = path.lastIndexOf('/');
        return path.substring(0, lastIndex + 1) + "sfu_rpkg/" + path.substring(lastIndex + 1);
    }

    static FileSystem createZip(Path path) throws IOException {
        Files.deleteIfExists(path);
        return FileSystems.newFileSystem(path, Map.of("create", "true"));
    }
}
