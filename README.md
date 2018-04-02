This package contains an updated implementation of the contract fragment pattern put forward by @JakeWharton that uses the parent fragment or the activity to check for a contract implementation. See the original idea [on jakes github](https://gist.github.com/JakeWharton/2621173)

This package also contains a base `Fragment` and `DialogFragment` implementation with bug fixes for child fragment animations and immersive dialog fragments. Learn more about these bugs [here](http://stackoverflow.com/questions/14900738/nested-fragments-disappear-during-transition-animation) and [here](https://stackoverflow.com/questions/32758559/maintain-immersive-mode-when-dialogfragment-is-shown)

If new fragment related ui issues pop up, I'll be sure to address them in these base classes.

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
    compile 'com.github.marksalpeter:contract-fragment:0.1
}
```

# Examples
```java 
import com.marksalpeter.fragment.ContractFragment;

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
    public static MyFragment newInstance() {
        MyFragment fragment = new MyFragment();
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	getContract().method();
    }
}
```
