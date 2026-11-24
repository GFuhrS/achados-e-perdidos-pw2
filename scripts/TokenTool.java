import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/** Gera chaves e JWTs locais de demonstração, sem dependências externas. */
public class TokenTool {
    private static final Path DIR = Path.of("secrets");
    private static final Path PRIVATE = DIR.resolve("privateKey.pem");
    private static final Path PUBLIC = DIR.resolve("publicKey.pem");
    private static final Base64.Encoder URL = Base64.getUrlEncoder().withoutPadding();

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("init")) {
            if (Files.exists(PRIVATE) || Files.exists(PUBLIC)) {
                if (!Files.exists(PRIVATE) || !Files.exists(PUBLIC)) {
                    throw new IllegalStateException("Par de chaves incompleto em secrets/");
                }
                System.out.println("Chaves já existem em secrets/");
                return;
            }
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            Files.createDirectories(DIR);
            writePem(PRIVATE, "PRIVATE KEY", pair.getPrivate().getEncoded());
            writePem(PUBLIC, "PUBLIC KEY", pair.getPublic().getEncoded());
            System.out.println("Chaves criadas em secrets/");
            return;
        }
        if (args.length >= 2 && args.length <= 3 && args[0].equals("token")) {
            String role = args[1];
            String name = args.length == 3 ? args[2] : role.toLowerCase();
            if ((!role.equals("Admin") && !role.equals("User")) || !name.matches("[A-Za-z0-9._-]+")) {
                throw new IllegalArgumentException("Uso: token Admin|User [nome-sem-espacos]");
            }
            long now = Instant.now().getEpochSecond();
            String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
            String payload = "{\"iss\":\"pw2-achados-e-perdidos\",\"sub\":\"" + name
                    + "\",\"upn\":\"" + name + "\",\"groups\":[\"" + role
                    + "\"],\"iat\":" + now + ",\"exp\":" + (now + 28800) + "}";
            String body = encode(header.getBytes(StandardCharsets.UTF_8)) + "."
                    + encode(payload.getBytes(StandardCharsets.UTF_8));
            String pem = Files.readString(PRIVATE);
            String base64 = pem.replaceAll("-----[^-]+-----|\\s", "");
            var key = KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(body.getBytes(StandardCharsets.US_ASCII));
            System.out.println(body + "." + encode(signature.sign()));
            return;
        }
        throw new IllegalArgumentException("Uso: init | token Admin|User [nome-sem-espacos]");
    }

    private static String encode(byte[] bytes) {
        return URL.encodeToString(bytes);
    }

    private static void writePem(Path path, String type, byte[] bytes) throws Exception {
        String content = "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(bytes)
                + "\n-----END " + type + "-----\n";
        Files.writeString(path, content, StandardCharsets.US_ASCII);
    }
}
