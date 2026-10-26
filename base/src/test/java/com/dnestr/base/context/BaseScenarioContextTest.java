package com.dnestr.base.context;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseScenarioContextTest {

    enum Key { USER, TOKEN }

    static class Context extends BaseScenarioContext<Key> {}

    private final Context context = new Context();

    @Test
    void typedKey_setGetContainsRemove() {
        context.set(Key.USER, "alice");
        assertThat(context.contains(Key.USER)).isTrue();
        assertThat((String) context.get(Key.USER)).isEqualTo("alice");

        context.remove(Key.USER);
        assertThat(context.contains(Key.USER)).isFalse();
        assertThat((String) context.get(Key.USER)).isNull();
    }

    @Test
    void dynamicStringKey_setGetContainsRemove() {
        context.set("session-id", "abc-123");
        assertThat(context.contains("session-id")).isTrue();
        assertThat((String) context.get("session-id")).isEqualTo("abc-123");

        context.remove("session-id");
        assertThat(context.contains("session-id")).isFalse();
    }

    @Test
    void typedAndDynamicMaps_areIndependent() {
        context.set(Key.TOKEN, "typed-value");
        context.set("TOKEN", "dynamic-value");

        assertThat((String) context.get(Key.TOKEN)).isEqualTo("typed-value");
        assertThat((String) context.get("TOKEN")).isEqualTo("dynamic-value");
    }

    @Test
    void clear_wipesBothMaps() {
        context.set(Key.USER, "alice");
        context.set("dynamic", "value");

        context.clear();

        assertThat(context.contains(Key.USER)).isFalse();
        assertThat(context.contains("dynamic")).isFalse();
    }

    @Test
    void getList_returnsEmptyList_whenKeyAbsent() {
        assertThat(context.<String>getList(Key.USER)).isEmpty();
    }

    @Test
    void addToList_createsListOnFirstCall() {
        context.addToList(Key.USER, "alice");

        assertThat(context.<String>getList(Key.USER)).containsExactly("alice");
    }

    @Test
    void addToList_appendsToExistingList() {
        context.addToList(Key.USER, "alice");
        context.addToList(Key.USER, "bob");

        assertThat(context.<String>getList(Key.USER)).containsExactly("alice", "bob");
    }

    @Test
    void addToList_doesNotAffectOtherKeys() {
        context.addToList(Key.USER, "alice");
        context.addToList(Key.TOKEN, "token-1");

        assertThat(context.<String>getList(Key.USER)).containsExactly("alice");
        assertThat(context.<String>getList(Key.TOKEN)).containsExactly("token-1");
    }

    @Test
    void getList_reflectsValuesAddedViaAddToList() {
        context.addToList(Key.USER, "alice");

        List<String> list = context.getList(Key.USER);

        assertThat(list).isEqualTo(context.get(Key.USER));
    }

    @Test
    void getList_whenKeyAbsent_doesNotStoreTheEmptyList() {
        context.<String>getList(Key.USER).add("alice");

        assertThat(context.contains(Key.USER)).isFalse();
    }

    @Test
    void getList_returnsLiveList_soMutationsAreStored() {
        context.addToList(Key.USER, "alice");

        context.<String>getList(Key.USER).add("bob");

        assertThat(context.<String>getList(Key.USER)).containsExactly("alice", "bob");
    }

    @Test
    void addToList_appendsToListStoredViaSet() {
        context.set(Key.USER, new ArrayList<>(List.of("alice")));

        context.addToList(Key.USER, "bob");

        assertThat(context.<String>getList(Key.USER)).containsExactly("alice", "bob");
    }

    @Test
    void addToList_whenKeyHoldsNonList_throwsClassCastException() {
        context.set(Key.USER, "alice");

        assertThatThrownBy(() -> context.addToList(Key.USER, "bob"))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void dynamicStringKey_addToListAndGetList() {
        context.addToList("ids", "1");
        context.addToList("ids", "2");

        assertThat(context.<String>getList("ids")).containsExactly("1", "2");
        assertThat(context.<String>getList("missing")).isEmpty();
    }

    @Test
    void addToList_typedAndDynamicKeysDoNotCollide() {
        context.addToList(Key.USER, "typed");
        context.addToList("USER", "dynamic");

        assertThat(context.<String>getList(Key.USER)).containsExactly("typed");
        assertThat(context.<String>getList("USER")).containsExactly("dynamic");
    }
}
