package peergos.server.tests;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import peergos.server.crypto.JniTweetNacl;

import java.util.*;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class TestJniTweetNacl {


    private static JniTweetNacl.Signer signer;
    private static JniTweetNacl.Symmetric symmetric;

    private static Random random = new Random(1337);

    @BeforeClass public static void init() {
        JniTweetNacl instance = JniTweetNacl.build();

        signer = new JniTweetNacl.Signer(instance);
        symmetric = new JniTweetNacl.Symmetric(instance);
        random.setSeed(1337);
    }


    public final int messageLength;

    public TestJniTweetNacl(int messageLength) {
        this.messageLength = messageLength;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        //spiral out
        int  i=1, j=1;
        int cutoff = 1024 * 1024;
        List<Integer> fibs  = new ArrayList<>();

        while  (i < cutoff) {
            int k = j;
            j = j+i;
            i = k;
            fibs.add(i);
        }
        return fibs.stream().map(e -> new Object[]{e})
                .collect(Collectors.toList());
    }

    @Test
    public void testSigningIdentity() {
        byte[] secretSignBytes = new byte[64];
        byte[] publicSignBytes = new byte[32];
        signer.crypto_sign_keypair(publicSignBytes, secretSignBytes);
        byte[] message = new byte[messageLength];
        random.nextBytes(message);
        byte[] signed = signer.crypto_sign(message, secretSignBytes);
        byte[] unsigned = signer.crypto_sign_open(signed, publicSignBytes);

        Assert.assertArrayEquals(message, unsigned);
        Assert.assertFalse(Arrays.equals(message, signed));
        Assert.assertFalse(Arrays.equals(signed, unsigned));
    }

    @Test
    public void testSecretboxIdentity() {
        byte[] key = new byte[32];
        byte[] nonce = new byte[32];
        random.nextBytes(key);
        random.nextBytes(nonce);

        byte[] message = new byte[messageLength];
        random.nextBytes(message);

        byte[] boxed = symmetric.secretbox(message, nonce, key);
        byte[] unboxed = symmetric.secretbox_open(boxed, nonce, key);
        Assert.assertArrayEquals(message, unboxed);
        Assert.assertFalse(Arrays.equals(message, boxed));
        Assert.assertFalse(Arrays.equals(boxed, unboxed));
    }

    @Test
    public void testSecretboxAsyncIdentity() {
        byte[] key = new byte[32];
        byte[] nonce = new byte[32];
        random.nextBytes(key);
        random.nextBytes(nonce);

        byte[] message = new byte[messageLength];
        random.nextBytes(message);
        byte[] boxed = symmetric.secretboxAsync(message, nonce, key).join();
        byte[] unboxed = symmetric.secretbox_openAsync(boxed, nonce, key).join();

        Assert.assertArrayEquals(message, unboxed);
        Assert.assertFalse(Arrays.equals(message, boxed));
        Assert.assertFalse(Arrays.equals(boxed, unboxed));
    }
}
