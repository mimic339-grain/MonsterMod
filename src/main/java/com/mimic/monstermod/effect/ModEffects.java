package com.mimic.monstermod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "monstermod");

    public static final RegistryObject<MobEffect> HOUKAI =
            MOB_EFFECTS.register("houkai", HoukaiEffect::new);
    public static final RegistryObject<MobEffect> BIND =
            MOB_EFFECTS.register("bind", BindEffect::new);
    // === 【Effectにするもの：一定時間状態が続くデバフ・バフ】 ===
    //bind　自分が作成したgeomodelで描画　動けないwasdが機能しないが攻撃は触れるし移動スキルも使える
    //stun　動けない　skill使えない
    //ぱららいず　一定時間　ランダムな時間ごとに2(変更有り)秒間動けないっていう定期的な動きの遮断
    //混乱wasdがwがs　aがｄ dがaなどと移動キーが変わる
    //霧　skillのクールタイムが見えずまだクールダウン中です　とは出る　ただguiでの枠線が真っ暗でいつ使えるかが直感的にわからない　食料ゲージや体力ゲージもわからない
    //出血　定期的にDamage
    //猛毒　通常マイクラ毒より早くダメージを受ける
    //チャーム？これはeffectか微妙だけど　かかった人は本元（かけた）の人から一定距離離れられない
    //腐敗　回復がDamageできない　nonheal
    //裂傷　スキル　wasdを受けるとDamage　
    //燃焼　ずっとwを押し続けるような移動になってしまう
    //沈黙　スキル使用不可能状態になる。
    //凍結　playerに殴られる（pvp不可や同チーム攻撃不可でも殴られたらok）またはmonsterの攻撃を受けるまで動けない　永遠
    //衰弱　最大hpの減少　一気に
    //無防備　次に受けるダメージが倍
    //防御力低下や攻撃力低下　これはeffectか？
    //瘴気　だんだんと最大hoの減少　だんだんとへって衰弱よりも減る
    //汚染　自分以外の特定範囲にいるplayerにダメージ
    //共鳴wasdが本元のwasdの影響を受ける　幸福吸血 ダメージ与えて回復
    //反射 ダメージを受けると跳ね返す
    //デバフ 全てのステータスが下がる
    //飢餓 スタミナがなくなる
    //弱体化 一定時間攻撃力ダウン(0でもいいかも)
    //呪い 呪い攻撃を受ければ受けるほど与えられるダメージが増幅する
    //憤怒 攻撃力があがる代わりに防御が減る
    //逆境 死に瀕するほど力があがる
    //激昂 攻撃間隔 攻撃力があがる
    //自傷 HPが減る代わりに一定時間攻撃力あっぷ
    //暴走 混乱状態になる代わりに攻撃力があがる
    //幸福とか？ 最大体力あっぷ
    //無敵 ダメージを受けない
    //堕落 一定時間毎に行動不能か、攻撃速度低下
    //→その代わり強化する形かデバフ系か
    //惰眠 一定時間毎に睡眠

    // === 【この下effectではない：瞬間のアクションやダメージ計算の属性】 ===

    //筋力低下　自分の持ってるアイテムを落とす
    //吸血 ダメージ与えて回復
    //吸収 ダメージを受けると回復
    //貫通 防御無視ダメージ
    //覚醒 覚醒した場合にallステータスアップ
}