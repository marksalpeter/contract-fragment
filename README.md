# How to install 
add the followig to gradle to install
```
allprojects{
    repositories {
        jcenter()
        maven { url "https://jitpack.io" }
    }
}
```
```
dependencies {
    ...
    compile 'com.github.marksalpeter:contract-fragment:1.0
}
```

# Example
```java 
import com.marksalpeter.contractfragment.ContractFragment;

public class MyFragment extends ContractFragment<Name.Contract> {

    public static final String TAG = Name.class.getSimpleName();

    /**
     * Contract has to be implemented by the parent fragment or the parent activity
     */
    public interface Contract {
		void method()
    }

    /**
     * newInterface returns a new fragment
     */
    public static Name newInstance() {
        Name fragment = new Name();
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		getContract().method();
    }
}
```