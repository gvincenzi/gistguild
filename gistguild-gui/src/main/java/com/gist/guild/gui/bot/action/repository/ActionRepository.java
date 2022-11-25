package com.gist.guild.gui.bot.action.repository;

import com.gist.guild.gui.bot.action.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActionRepository extends JpaRepository<Action, Long> {
    Optional<Action> findByTelegramUserIdAndInProgressTrue(Long telegramUserId);
}
