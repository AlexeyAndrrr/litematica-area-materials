package dev.areamaterials;

import fi.dy.masa.litematica.tool.ToolMode;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AreaToolModeTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test void appendsTheTenthModeAndPreservesTheOriginalNine() {
        assertEquals(List.of("AREA_SELECTION", "SCHEMATIC_PLACEMENT", "FILL", "REPLACE_BLOCK", "PASTE_SCHEMATIC",
                        "GRID_PASTE", "MOVE", "DELETE", "REBUILD", "AREA_MATERIALS"),
                java.util.Arrays.stream(ToolMode.values()).map(Enum::name).toList());
        assertEquals(9, AreaToolMode.get().ordinal());
        assertTrue(AreaToolMode.get().getUsesSchematic());
        assertFalse(AreaToolMode.get().getUsesBlockPrimary());
        assertFalse(AreaToolMode.get().getUsesBlockSecondary());
    }

    @Test void isReachableInBothDirectionsAndWrapsToTheFirstMode() {
        PlayerEntity player = player(true);
        assertSame(AreaToolMode.get(), ToolMode.REBUILD.cycle(player, true));
        assertSame(AreaToolMode.get(), ToolMode.AREA_SELECTION.cycle(player, false));
        assertSame(ToolMode.AREA_SELECTION, AreaToolMode.get().cycle(player, true));
        assertSame(ToolMode.REBUILD, AreaToolMode.get().cycle(player, false));
    }

    @Test void creativeCycleIncludesAllTenModesExactlyOnce() {
        assertEquals(Set.of(ToolMode.values()), cycle(player(true), true));
        assertEquals(Set.of(ToolMode.values()), cycle(player(true), false));
    }

    @Test void survivalCycleIncludesAreaMaterialsAndSkipsCreativeOperations() {
        Set<ToolMode> expected = Set.of(ToolMode.AREA_SELECTION, ToolMode.SCHEMATIC_PLACEMENT,
                ToolMode.REBUILD, AreaToolMode.get());
        assertEquals(expected, cycle(player(false), true));
        assertEquals(expected, cycle(player(false), false));
    }

    @Test void canBeRestoredByTheNameLitematicaSaves() {
        assertSame(AreaToolMode.get(), ToolMode.valueOf(AreaToolMode.get().name()));
        assertSame(AreaToolMode.get(), Enum.valueOf(ToolMode.class, "AREA_MATERIALS"));
    }

    private Set<ToolMode> cycle(PlayerEntity player, boolean forward) {
        Set<ToolMode> modes = new LinkedHashSet<>();
        ToolMode mode = ToolMode.AREA_SELECTION;
        do {
            assertTrue(modes.add(mode), "Cycle repeated a mode before returning to the first one");
            mode = mode.cycle(player, forward);
            assertTrue(modes.size() <= 10, "Cycle did not wrap");
        } while (mode != ToolMode.AREA_SELECTION);
        return modes;
    }

    private PlayerEntity player(boolean creative) {
        PlayerAbilities abilities = new PlayerAbilities();
        abilities.creativeMode = creative;
        PlayerEntity player = mock(PlayerEntity.class);
        when(player.getAbilities()).thenReturn(abilities);
        return player;
    }
}
