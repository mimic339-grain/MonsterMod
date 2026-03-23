package com.mimic.monstermod.skill;

public enum SkillType {//これはいつ効果が付与するかっていう付与タイミング たとえmoveやstrikeでも誰かへ向けてかや効果持続時間はそれのskill次第
    // 攻撃スキルはほぼプレビュー時間があって、rootcasterがあるそしてattackpreview.pngで貼る　攻撃後硬直がある monster用
    //MOVEMENTはrootcasterがない　プレビューもない　攻撃後硬直がない　自分への付与
    //
    // とかっていったけどこれらは技同士の関わり合いではなくてbuildしていくうえで今後自分対象とか自分含めて対象とか自分以外対象とか指定した１にん対象とかっていう対象をskillleadにいれるかもしれない　uuidteam対象とかそういう場合category
    //でわけてるほうが一つ一つ設定するのではなくtある程度設定してるのものを用意してそれに当てはめていくっていうのができるからskillごとのskillleadの特性によっての分類である　バフ　デバフっていう意味ではない　skillleadの数値　true falseの一括設定弁利用
    // TOUCHは効果が自分で
    NONE,
    STRIKE,         // 攻撃スキル
    MOVEMENT,           // 回避　移動スキル　
    // CANCELかNORMALかMOVE　CANCELの場合は移動最優先　MOVEの場合は移動空中しながらcomboやnormalにつなげる攻撃前の移動用　SPECIALは日常で使うものでnormal comboには繋がらないけどcdが短いっていう移動専門　
    TOUCH,          // 接触スキル（★TODO: 自分を赤く発光させ、触れた相手にダメージ）
    SPECIAL;        // その他

    public enum Category {
        NORMAL,//強攻撃　弱攻撃は通常マイクラ殴りふっとばしたり付与したり
        COMBO,//normalやdashに続くものでcombo単体では使えない　ただその分攻撃後硬直が大きい　ただ見づらいし不意打ちができる　また、normalスキル派生だった場合はrootcasterをキャンセルもできる　動的なskill
        CANCEL,//緊急回避用　cdは重いけど
        UNIQUE,//大技comboに派生させないようの攻撃や特殊効果　単純の移動とかなんでもござれ　これは単体で使いたい場合　only
        DASH,//攻撃normal comboへの派生目的　移動からの攻撃っていうダッシュ攻撃が可能
        MOVE//単純移動用でcdがはやい　移動したり
    }
}