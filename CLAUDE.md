# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

MonsterMod (`com.mimic.monstermod`) は Minecraft Forge 用 Java MOD。
- Minecraft: 1.20.1 / Forge: 47.4.10 / Java: 17 (toolchain)
- Mixin (SpongePowered, 0.8.5, `JAVA_17` compatibility) を使用してバニラ・Forge の挙動を書き換えている
- GeckoLib (`geckolib-forge-1.20.1:4.4.9`) でエンティティ・アイテムのアニメーション/レンダリングを行う
- `player-animation-lib-forge` でプレイヤーのポーズ/アニメーションレイヤーを制御
- Mixin 設定: `src/main/resources/monstermod.mixins.json`（クライアント専用 Mixin は `mixin/player/*`、アクセサは `mixin/accessor/*`）

目的
大人数用であるためネットワークの管理で自分だけの視点ではなく基本全playerに変身やスキル　クールダウンが同期される


## ビルド・実行コマンド

Windows なので PowerShell 経由で `gradlew.bat` を使う。

```bash
./gradlew.bat build
```

```bash
./gradlew.bat compileJava
```

クライアント起動（動作確認用）:
```bash
./gradlew.bat runClient
```

サーバー起動:
```bash
./gradlew.bat runServer
```

依存関係のキャッシュがおかしい場合:
```bash
./gradlew.bat --refresh-dependencies
```

このリポジトリに専用の lint/test タスクは定義されていない（Forge MDK 標準構成）。テストを追加する場合は `build.gradle` にテストフレームワークの導入から必要になる。

## ワークフロー（必須）

- コードを変更したら必ず `./gradlew.bat compileJava`（大きめの変更は `build`）を実行し、エラーが出ないか確認する。
- エラーが出た場合は修正が完了するまで作業完了と報告しない。
- ビルドが通ったことを確認してから次のタスクに進む。
- 編集した際メソッドの上に//で日本語でコメントを書きどのような内容のコードでどこのファイルのどのメソッドに繋がっているのかを書くこと　場合によってはどの内容のコードなのかだけでもいいとする
- 重要　編集前に私のチャットの内容をまとめて認識が正しいかの確認をする→編集後、編集した内容をまとめて次のチャットで何を次に行うべきか選択肢を出しつつ次のワークの提案する

## 編集ルール
- コードを変更する前に、必ず新しい git ブランチを切ってから作業する（例: `git checkout -b feature/effect-tool`）。
- 既存ファイルを直接上書きせず、変更はコミット単位で記録する。
- 大きな変更に着手する前に `git commit` で現状を保存しておく。
- mixinの内容がとても難しい可能性があるため気をつけてほしい


## アーキテクチャ
### エントリーポイントと初期化
- `MonsterMod.java` が `@Mod` メインクラス。`init/` パッケージ配下の各 `Register`/`ModXxx` クラス（`ModItems`, `ModEntitieType`, `ModEntityAttributes`, `ModParticles`, `ModGuiRegister`, `EntityRendererRegister`, `SkillEffectRegistry`, `IdentityType` など）を通じて Forge のレジストリイベントに登録する構成。

### 変身（Identity）システム
プレイヤーが「モンスター」または「ハンター」の姿に変身するシステムが中核機能。
- `identity/BaseIdentity.java` が変身キャラクターの基底クラス
- `identity/monster/` 配下（`MimicIdentity`, `YatagarasuIdentity` など）が個々のモンスター実装
- `identity/BaseMonsterIdentityRegistry.java` が利用可能な Identity を登録・解決するレジストリ
- 変身状態は Forge Capability で永続化・同期される: `capability/MonsterTransformation.java`（+ `*Provider`）。プレイヤーの HP・変身種別などがここに乗る
- HP の読み書きキーは必ず `MonsterTransformation#getHpKey()` を使う（`identity.getId()` は descriptionId 形式で別物のため混在させない）
- 変身の同期はネットワークパケット (`network/server/S2CTransformSyncPacket.java`, `PlayerTransformC2SPacket.java`) で行われる

### 戦闘・スキルシステム
- `skill/SkillId.java` / `skill/SkillType.java` でスキルを識別・分類
- `skill/SkillLead.java` + `skill/SkillLeadRegistry.java` がスキルの発動ロジック（"lead"）を保持するレジストリパターン
- `skill/SkillEffectSpec.java` / `PotionEffectSpec.java` / `DamageType.java` がスキル効果の宣言的定義
- クライアント→サーバーのスキル発動要求は `network/client/C2S_SkillCastRequestPacket.java`。拒否時は `S2C_SkillCastRejectedPacket` でクライアントの仮ロックを解除する
- モンスター側の攻撃判定/プレビューは `entity/monster/` と `S2C_SpawnSkillLeadPacket.java` に関連
- 部位ごとの当たり判定は `entity/hitbox/`（GeckoLibのボーンに追従。実体モンスターと変身プレイヤーの両方で共有）

### エンティティ・レンダリング (GeckoLib)
- `entity/` 配下にエンティティ本体、`entity/monster/` はモンスター種族ごとの実装、`entity/layer/` は装備・羽など ArmorLayer 相当のレンダリングレイヤー、`entity/render/` はレンダラー
- `geo/model/`（`monster/`, `item/`, `layer/`）と `geo/renderer/`（`item/`）が GeckoLib のモデル/レンダラー実装。モデル・アニメーション定義は `src/main/resources/assets/monstermod/geo/*.geo.json`, `animations/*.animation.json`
- テクスチャは `src/main/resources/assets/monstermod/textures/`

### ネットワーク層
- `network/ModMessages.java` がパケット登録の集約ポイント（Forge `SimpleChannel`）
- 命名規則: クライアント→サーバーは `C2S*`、サーバー→クライアントは `S2C*`。新規パケットを追加する場合は `ModMessages` への登録を忘れないこと
- プレイヤー個別指定（特定プレイヤーだけに変身やスキルを効かせる）の仕組みが `player指定システム` として存在（`network`/`capability` 層をまたぐ）

### Mixin
- クライアント限定の Mixin が大半（`mixin/player/*`）。プレイヤーの見た目・入力・HUD・ダメージインジケータ・アイテム表示などバニラ挙動をフックしている
- `mixin/accessor/*` はバニラ private フィールド/メソッドへのアクセサ（`EntityAccessor`, `RenderStateShardAccessor`）
- `RenderStateShardAccessor` は現在未使用だが、今後のエフェクト（加算 RenderType）実装用に残してある
- Mixin を追加/削除したら `monstermod.mixins.json` の `mixins`/`client` 配列を必ず更新する

### GUI
- `overlay/` と `gui/` が HUD 描画（体力バー等）
- 
### Effect作成の注意点
-　ビームの板をすべて表裏1回ずつ描くようにしましたのように表裏で一回ずつ描く
-　VfxRenderUtil に使い分けの注意書きを残しました（裏面を捨てる設定では quad ではなく quadBothSides を使う）

## 現在の構成メモ

- **Hunter（変身・武器・コンボ）系は削除済み**。Monster 変身のみ。復活させる場合は履歴を参照すること
- アニメーションと当たり判定の位相は `level.getGameTime()` を基準に共有している。
  GeckoLib 側は `BaseEntity#getTick()` と `PhaseLockedAnimationController#forcePhase` で
  同じ時間軸に固定しているため、この2つを崩さないこと（崩すと見た目と判定がズレる）
