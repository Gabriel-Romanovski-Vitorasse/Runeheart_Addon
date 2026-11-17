package com.gabriel.runeheart;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import java.util.List;

/** Pergaminho de runa: usar ensina a runa guardada nele (tag {@code rune}) e o consome. Achado em baús de estruturas. */
public class RuneScrollItem extends Item {
    public RuneScrollItem() {
        super(new Item.Properties().tab(ItemGroup.TAB_MISC).stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    public static Rune runeOf(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        return tag == null ? null : Rune.byId(tag.getString("rune"));
    }

    public static ItemStack of(Rune rune) {
        ItemStack stack = new ItemStack(RuneheartMod.RUNE_SCROLL.get());
        stack.getOrCreateTag().putString("rune", rune.id);
        return stack;
    }

    @Override
    public ITextComponent getName(ItemStack stack) {
        Rune r = runeOf(stack);
        return r == null ? super.getName(stack)
                : new TranslationTextComponent("item.runeheart.rune_scroll.named", new TranslationTextComponent(r.langKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        Rune r = runeOf(stack);
        if (r == null) return;
        tooltip.add(new TranslationTextComponent(r.core ? "tooltip.runeheart.core" : "tooltip.runeheart.modifier")
                .withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent(r.langKey() + ".desc").withStyle(TextFormatting.DARK_AQUA));
    }

    @Override
    public void fillItemCategory(ItemGroup group, NonNullList<ItemStack> items) {
        if (!allowdedIn(group)) return;
        for (Rune r : Rune.values()) items.add(of(r));
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Rune r = runeOf(stack);
        if (r == null) return ActionResult.fail(stack);
        if (world.isClientSide) return ActionResult.success(stack);
        ServerPlayerEntity sp = (ServerPlayerEntity) player;
        if (!RuneData.learn(sp, r)) {
            Msg.bar(sp, TextFormatting.GRAY, "scroll_known", new TranslationTextComponent(r.langKey()));
            return ActionResult.fail(stack);
        }
        if (!player.abilities.instabuild) stack.shrink(1);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0F, 0.8F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.7F, 1.4F);
        Msg.chat(sp, TextFormatting.AQUA, "scroll_learned", new TranslationTextComponent(r.langKey()));
        RuneData.sync(sp);
        return ActionResult.consume(stack);
    }
}
