package com.mimic.monstermod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 血石。特定のプレイヤーの居場所を追うための石。
 *
 * 【使い方】
 * 1. 何も入っていない状態でプレイヤーを右クリックすると、その相手の血を取り込む
 * 2. 血の入った石を手に持っている間だけ、足元に相手の方角を指す矢印が出る
 *    (距離・方角・高さの差も一緒に表示する)
 *
 * 【相手をアイテムのNBTで持つ理由】
 * 「誰を追っているか」は石そのものの性質なので、石に書いておくのが素直。
 * プレイヤー側に持たせると、石を渡したときに追跡先が付いてこない。
 * NBTに入れておけば、チェストに入れても人に渡しても対象が保たれる。
 *
 * 相手の座標そのものはここには入れない。常に最新でなければ意味がないので、
 * 持っている間だけサーバーから送る({@link com.mimic.monstermod.item.BloodStoneEvents})。
 *
 * 見た目(空 / 血入り)の切り替えは、モデルの override で行っている。
 * 判定用のプロパティ登録は {@link com.mimic.monstermod.events.ModClientEvents}。
 */
public class BloodStoneItem extends Item {

    /** 追う相手のUUID */
    private static final String TAG_TARGET = "BloodTarget";
    /** 表示用に控えておく相手の名前。相手がオフラインでも名前だけは出せる */
    private static final String TAG_TARGET_NAME = "BloodTargetName";

    // プレイヤーのインベントリ画面でのスロット番号。
    // ホットバーは36〜44、左手は45と決まっている(バニラの InventoryMenu の並び)
    private static final int INVENTORY_MENU_HOTBAR_START = 36;
    private static final int INVENTORY_MENU_OFFHAND = 45;

    public BloodStoneItem() {
        // 1個しか重ねられない。まとめて持てると「どの石が誰か」が分からなくなる
        super(new Item.Properties().stacksTo(1));
    }

    /**
     * プレイヤーを右クリックしたときに、その相手を追跡先として書き込む。
     * 呼び出し元: バニラの Player#interactOn（相手がLivingEntityのとき呼ばれる）
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user,
                                                  LivingEntity target, InteractionHand hand) {
        // 相手はプレイヤーのみ。Mobを追う用途は今は無い
        if (!(target instanceof Player marked)) return InteractionResult.PASS;
        if (marked == user) return InteractionResult.PASS;

        if (user.level().isClientSide) {
            // 見た目の腕振りだけクライアントで先に出す
            return InteractionResult.SUCCESS;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_TARGET, marked.getUUID());
        tag.putString(TAG_TARGET_NAME, marked.getGameProfile().getName());

        // 【重要】書き換えた石をその場でクライアントへ送り直す。
        //
        // インベントリの中身は毎tickバニラが差分を見て送ってくれるはずだが、
        // エンティティへの右クリックで手持ちのNBTだけを書き換えた場合、
        // クライアント側に反映されないことがある。
        // 反映されないと、名前も見た目も変わらず、
        // クライアント側で「血が入っているか」を見ている処理が全部素通りしてしまう。
        // ここで明示的に該当スロットだけ送り直しておけば、経路に関係なく必ず届く。
        if (user instanceof ServerPlayer sp) {
            int menuSlot = (hand == InteractionHand.MAIN_HAND)
                    ? INVENTORY_MENU_HOTBAR_START + sp.getInventory().selected
                    : INVENTORY_MENU_OFFHAND;
            sp.connection.send(new ClientboundContainerSetSlotPacket(
                    sp.inventoryMenu.containerId,
                    sp.inventoryMenu.incrementStateId(),
                    menuSlot, stack));
        }

        user.displayClientMessage(
                Component.literal(marked.getGameProfile().getName() + " の血を取り込んだ"), true);
        return InteractionResult.CONSUME;
    }

    // ===== NBTの読み書き。他のクラスからはここを通す =====

    /** 追跡先のUUID。まだ何も入っていなければ null */
    @Nullable
    public static UUID getTarget(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(TAG_TARGET)) return null;
        return tag.getUUID(TAG_TARGET);
    }

    /** 追跡先の名前。控えが無ければ空文字 */
    public static String getTargetName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TAG_TARGET_NAME);
    }

    /** 血が入っているか（= 追跡先が設定されているか） */
    public static boolean isFilled(ItemStack stack) {
        return getTarget(stack) != null;
    }

    /**
     * 手に持っている血の入った石を返す。利き手→反対の手の順で探す。
     * 両手に持っていても矢印は1つでよいので、最初に見つかったものを使う。
     */
    @Nullable
    public static ItemStack findHeld(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof BloodStoneItem && isFilled(main)) return main;

        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof BloodStoneItem && isFilled(off)) return off;

        return null;
    }

    // ===== 表示 =====

    /** 血が入ると名前が変わる。誰の血かも名前に出す */
    @Override
    public Component getName(ItemStack stack) {
        if (!isFilled(stack)) return super.getName(stack);
        return Component.translatable("item.monstermod.blood_stone.filled", getTargetName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (isFilled(stack)) {
            tooltip.add(Component.translatable("item.monstermod.blood_stone.tip.filled")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.monstermod.blood_stone.tip.empty")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
