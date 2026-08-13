package com.mimic.monstermod.item;

import com.mimic.monstermod.dialogue.DialogueBinding;
import com.mimic.monstermod.dialogue.DialoguePage;
import com.mimic.monstermod.dialogue.DialogueSet;
import com.mimic.monstermod.dialogue.DialogueStore;
import com.mimic.monstermod.dialogue.PortraitSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 会話設定アイテム。ゲーム内で会話を作り、エンティティに紐付けるための道具。
 *
 * 使い方:
 *  1. アイテムに会話IDを設定する      : 名前を付けた金床済みアイテムを持って右クリック(空中)
 *                                      → アイテム名がそのまま会話IDになる
 *  2. 本(署名済み)から本文を取り込む  : オフハンドに本を持って右クリック(空中)
 *                                      → 本の1ページ = 会話1ページとして登録
 *  3. エンティティに紐付ける          : 対象を右クリック
 *                                      → その村人/プレイヤーを右クリックで会話が出るようになる
 *
 * 長文入力はMinecraftのGUIでは苦痛なため、バニラの「本と羽根ペン」を
 * そのままエディタとして流用している(複数ページ・改行がそのまま使える)。
 *
 * 話者名・立ち絵・効果音・文体は、アイテムのNBTに保持した設定を取り込み時に適用する。
 * (設定は /dialogue コマンドや今後の設定GUIから変更できる想定)
 */
public class DialogueEditorItem extends Item {

    private static final String TAG_ID       = "dialogue_id";
    private static final String TAG_NAME     = "speaker_name";
    private static final String TAG_PORTRAIT = "portrait";
    private static final String TAG_SOUND    = "sound";
    private static final String TAG_STYLE    = "style";

    public DialogueEditorItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // ---- アイテムNBTの読み書き ----
    public static String getDialogueId(ItemStack stack) {
        CompoundTag t = stack.getOrCreateTag();
        String id = t.getString(TAG_ID);
        // 未設定ならアイテム名(金床でリネームしたもの)をIDとして使う
        if (id.isEmpty() && stack.hasCustomHoverName()) {
            id = stack.getHoverName().getString().trim().replace(' ', '_');
        }
        return id;
    }

    public static void setDialogueId(ItemStack stack, String id) {
        stack.getOrCreateTag().putString(TAG_ID, id);
    }

    public static String getSpeakerName(ItemStack stack) {
        return stack.getOrCreateTag().getString(TAG_NAME);
    }

    public static void setSpeakerName(ItemStack stack, String name) {
        stack.getOrCreateTag().putString(TAG_NAME, name);
    }

    public static void setPortrait(ItemStack stack, PortraitSpec spec) {
        stack.getOrCreateTag().put(TAG_PORTRAIT, spec.save());
    }

    public static PortraitSpec getPortrait(ItemStack stack) {
        CompoundTag t = stack.getOrCreateTag();
        if (!t.contains(TAG_PORTRAIT)) return PortraitSpec.NONE;
        return PortraitSpec.load(t.getCompound(TAG_PORTRAIT));
    }

    public static void setSound(ItemStack stack, String soundId) {
        stack.getOrCreateTag().putString(TAG_SOUND, soundId);
    }

    public static String getSound(ItemStack stack) {
        return stack.getOrCreateTag().getString(TAG_SOUND);
    }

    public static void setStyle(ItemStack stack, DialoguePage.TextStyle style) {
        stack.getOrCreateTag().putString(TAG_STYLE, style.name());
    }

    public static DialoguePage.TextStyle getStyle(ItemStack stack) {
        try {
            return DialoguePage.TextStyle.valueOf(stack.getOrCreateTag().getString(TAG_STYLE));
        } catch (IllegalArgumentException e) {
            return DialoguePage.TextStyle.NORMAL;
        }
    }

    /**
     * 空中で右クリック: オフハンドの本から会話を取り込む。
     * 本が無い場合は現在の設定内容を表示するだけ。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        String id = getDialogueId(stack);
        if (id.isEmpty()) {
            player.displayClientMessage(Component.literal(
                    "金床でこのアイテムに名前を付けてください(その名前が会話IDになります)")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResultHolder.fail(stack);
        }

        ItemStack book = player.getOffhandItem();
        if (book.is(Items.WRITTEN_BOOK) || book.is(Items.WRITABLE_BOOK)) {
            int pages = importFromBook(player, stack, book, id);
            player.displayClientMessage(Component.literal(
                    "会話 '" + id + "' を登録しました(" + pages + "ページ)")
                    .withStyle(ChatFormatting.GREEN), false);
        } else {
            player.displayClientMessage(Component.literal(
                    "会話ID: " + id + " / 話者: " + (getSpeakerName(stack).isEmpty() ? "(未設定)" : getSpeakerName(stack))
                            + " / 文体: " + getStyle(stack)
                            + "  ※オフハンドに本を持って使うと本文を取り込みます"), false);
        }
        return InteractionResultHolder.success(stack);
    }

    /** 本の各ページを会話ページとして取り込む。既存の同IDは上書きする */
    private int importFromBook(Player player, ItemStack editor, ItemStack book, String id) {
        DialogueStore store = DialogueStore.get(player.getServer());
        DialogueSet set = new DialogueSet(id);

        String speaker = getSpeakerName(editor);
        PortraitSpec portrait = getPortrait(editor);
        String sound = getSound(editor);
        DialoguePage.TextStyle style = getStyle(editor);

        CompoundTag bookTag = book.getTag();
        if (bookTag != null && bookTag.contains("pages")) {
            ListTag pages = bookTag.getList("pages", Tag.TAG_STRING);
            for (int i = 0; i < pages.size(); i++) {
                String raw = pages.getString(i);
                // 署名済みの本はJSON形式で入っているため、素のテキストに寄せる
                String text = raw.startsWith("{") || raw.startsWith("\"")
                        ? Component.Serializer.fromJson(raw) != null
                            ? Component.Serializer.fromJson(raw).getString()
                            : raw
                        : raw;
                set.addPage(new DialoguePage(speaker, text, portrait, sound, style));
            }
        }
        store.put(set);
        return set.getPages().size();
    }

    /**
     * エンティティを右クリック: そのエンティティに会話を紐付ける。
     * 以後、その村人/プレイヤーを右クリックすると会話が始まる。
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;

        String id = getDialogueId(stack);
        if (id.isEmpty()) {
            player.displayClientMessage(Component.literal("先に会話IDを設定してください")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.FAIL;
        }

        if (player.isShiftKeyDown()) {
            DialogueBinding.set(target, null);
            player.displayClientMessage(Component.literal("会話の紐付けを解除しました")
                    .withStyle(ChatFormatting.YELLOW), false);
        } else {
            DialogueBinding.set(target, id);
            player.displayClientMessage(Component.literal(
                    target.getName().getString() + " に会話 '" + id + "' を設定しました")
                    .withStyle(ChatFormatting.GREEN), false);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        String id = getDialogueId(stack);
        tooltip.add(Component.literal("会話ID: " + (id.isEmpty() ? "(金床で命名してください)" : id))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("空中で右クリック: オフハンドの本から取り込み").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("エンティティに右クリック: 会話を紐付け").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift+右クリック: 紐付け解除").withStyle(ChatFormatting.DARK_GRAY));
    }
}
